package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.infrastructure.entity.*;
import com.optical.net.sisplus.app.infrastructure.repository.*;
import com.optical.net.sisplus.app.infrastructure.web.cargo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CargoService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final CargoLoadRepository cargoLoadRepository;
    private final CargoSettlementRepository cargoSettlementRepository;
    private final CompanyExpenseService companyExpenseService;

    public CargoService(DriverRepository driverRepository,
                        VehicleRepository vehicleRepository,
                        CargoLoadRepository cargoLoadRepository,
                        CargoSettlementRepository cargoSettlementRepository,
                        CompanyExpenseService companyExpenseService) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.cargoLoadRepository = cargoLoadRepository;
        this.cargoSettlementRepository = cargoSettlementRepository;
        this.companyExpenseService = companyExpenseService;
    }

    /* ── Drivers ── */
    public List<DriverResponse> getAllDrivers() {
        return driverRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toDriverResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DriverResponse createDriver(DriverRequest request) {
        Driver driver = Driver.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .active(request.getActive() == null || request.getActive())
                .build();
        return toDriverResponse(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponse updateDriver(Long id, DriverRequest request) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));
        driver.setName(request.getName());
        driver.setPhone(request.getPhone());
        if (request.getActive() != null) driver.setActive(request.getActive());
        return toDriverResponse(driverRepository.save(driver));
    }

    @Transactional
    public String deleteDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));
        if (vehicleRepository.countByDriverId(id) > 0) {
            driver.setActive(false);
            driverRepository.save(driver);
            return "Conductor desactivado porque tiene carros asignados";
        }
        driverRepository.deleteById(id);
        return "Conductor eliminado";
    }

    /* ── Vehicles ── */
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toVehicleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        Driver driver = request.getDriverId() != null
                ? driverRepository.findById(request.getDriverId()).orElse(null)
                : null;
        Vehicle vehicle = Vehicle.builder()
                .plate(request.getPlate())
                .name(request.getName())
                .driver(driver)
                .active(request.getActive() == null || request.getActive())
                .build();
        return toVehicleResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));
        vehicle.setPlate(request.getPlate());
        vehicle.setName(request.getName());
        if (request.getDriverId() != null) {
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));
            vehicle.setDriver(driver);
        }
        if (request.getActive() != null) vehicle.setActive(request.getActive());
        return toVehicleResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public String deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));
        if (cargoLoadRepository.countByVehicleId(id) > 0) {
            vehicle.setActive(false);
            vehicleRepository.save(vehicle);
            return "Vehículo desactivado porque tiene cargues registrados";
        }
        vehicleRepository.deleteById(id);
        return "Vehículo eliminado";
    }

    /* ── Cargo Loads ── */
    public List<CargoLoadResponse> getLoadsByDate(LocalDate date) {
        if (date == null) date = LocalDate.now();
        return cargoLoadRepository.findByLoadDateWithSettlement(date).stream()
                .map(this::toCargoLoadResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CargoLoadResponse createLoad(CargoLoadRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));

        cargoLoadRepository.findByVehicleIdAndLoadDate(vehicle.getId(), request.getLoadDate())
                .ifPresent(existing -> { throw new RuntimeException("Ya existe un cargue para este carro en la fecha seleccionada"); });

        CargoLoad load = CargoLoad.builder()
                .vehicle(vehicle)
                .loadDate(request.getLoadDate())
                .merchandiseValue(request.getMerchandiseValue())
                .notes(request.getNotes())
                .build();
        return toCargoLoadResponse(cargoLoadRepository.save(load));
    }

    @Transactional
    public CargoLoadResponse updateLoad(Long id, CargoLoadRequest request) {
        CargoLoad load = cargoLoadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cargue no encontrado"));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));
        load.setVehicle(vehicle);
        load.setLoadDate(request.getLoadDate());
        load.setMerchandiseValue(request.getMerchandiseValue());
        load.setNotes(request.getNotes());
        return toCargoLoadResponse(cargoLoadRepository.save(load));
    }

    @Transactional
    public CargoLoadResponse markDelivered(Long id) {
        CargoLoad load = cargoLoadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cargue no encontrado"));
        load.setStatus(CargoStatus.ENTREGADO);
        return toCargoLoadResponse(cargoLoadRepository.save(load));
    }

    @Transactional
    public CargoLoadResponse markPending(Long id) {
        CargoLoad load = cargoLoadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cargue no encontrado"));
        load.setStatus(CargoStatus.PENDIENTE);
        return toCargoLoadResponse(cargoLoadRepository.save(load));
    }

    @Transactional
    public void deleteLoad(Long id) {
        cargoLoadRepository.deleteById(id);
    }

    /* ── Settlements ── */
    @Transactional
    public CargoLoadResponse createOrUpdateSettlement(CargoSettlementRequest request) {
        CargoLoad load = cargoLoadRepository.findById(request.getCargoLoadId())
                .orElseThrow(() -> new RuntimeException("Cargue no encontrado"));

        CargoSettlement settlement = cargoSettlementRepository.findByCargoLoadId(load.getId())
                .orElse(CargoSettlement.builder().cargoLoad(load).build());

        settlement.setDeliveredValue(request.getDeliveredValue());
        settlement.setReturnedValue(request.getReturnedValue());
        settlement.setCoins(request.getCoins());
        settlement.setCash(request.getCash());
        settlement.setQr(request.getQr());
        settlement.setSecurity(request.getSecurity());
        settlement.calculateTotal();

        cargoSettlementRepository.save(settlement);
        load.setSettlement(settlement);
        return toCargoLoadResponse(load);
    }

    /* ── Report ── */
    public CargoReportResponse getReportByDate(LocalDate date) {
        if (date == null) date = LocalDate.now();
        List<CargoLoad> loads = cargoLoadRepository.findByLoadDateWithSettlement(date);
        List<CargoLoadResponse> responses = loads.stream()
                .map(this::toCargoLoadResponse)
                .collect(Collectors.toList());

        double totalMerchandise = sum(loads, CargoLoad::getMerchandiseValue);
        double totalDelivered = sumSettlements(loads, CargoSettlement::getDeliveredValue);
        double totalReturned = sumSettlements(loads, CargoSettlement::getReturnedValue);
        double totalCoins = sumSettlements(loads, CargoSettlement::getCoins);
        double totalCash = sumSettlements(loads, CargoSettlement::getCash);
        double totalQr = sumSettlements(loads, CargoSettlement::getQr);
        double totalSecurity = sumSettlements(loads, CargoSettlement::getSecurity);
        double grandTotal = sumSettlements(loads, CargoSettlement::getTotal);

        long deliveredCount = loads.stream().filter(l -> l.getStatus() == CargoStatus.ENTREGADO).count();
        long pendingCount = loads.size() - deliveredCount;

        List<CompanyExpenseResponse> dayExpenses = companyExpenseService.findByFilters(date, date, null, null, null, null);
        double totalExpenses = dayExpenses.stream().mapToDouble(e -> e.getAmount() == null ? 0.0 : e.getAmount()).sum();
        Map<String, Double> expensesByCategory = dayExpenses.stream()
                .filter(e -> e.getCategory() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().getName(),
                        Collectors.summingDouble(e -> e.getAmount() == null ? 0.0 : e.getAmount())
                ));

        return CargoReportResponse.builder()
                .date(date)
                .loads(responses)
                .totalMerchandise(totalMerchandise)
                .totalDelivered(totalDelivered)
                .totalReturned(totalReturned)
                .totalCoins(totalCoins)
                .totalCash(totalCash)
                .totalQr(totalQr)
                .totalSecurity(totalSecurity)
                .grandTotal(grandTotal)
                .deliveredCount(deliveredCount)
                .pendingCount(pendingCount)
                .totalExpenses(totalExpenses)
                .expensesByCategory(expensesByCategory)
                .build();
    }

    /* ── Mappers ── */
    private DriverResponse toDriverResponse(Driver driver) {
        return DriverResponse.builder()
                .id(driver.getId())
                .name(driver.getName())
                .phone(driver.getPhone())
                .active(driver.isActive())
                .createdAt(driver.getCreatedAt())
                .build();
    }

    private VehicleResponse toVehicleResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .plate(vehicle.getPlate())
                .name(vehicle.getName())
                .driver(vehicle.getDriver() != null ? toDriverResponse(vehicle.getDriver()) : null)
                .active(vehicle.isActive())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }

    private CargoLoadResponse toCargoLoadResponse(CargoLoad load) {
        return CargoLoadResponse.builder()
                .id(load.getId())
                .vehicle(toVehicleResponse(load.getVehicle()))
                .loadDate(load.getLoadDate())
                .merchandiseValue(load.getMerchandiseValue())
                .status(load.getStatus().name())
                .notes(load.getNotes())
                .createdAt(load.getCreatedAt())
                .settlement(load.getSettlement() != null ? toSettlementResponse(load.getSettlement()) : null)
                .build();
    }

    private CargoSettlementResponse toSettlementResponse(CargoSettlement settlement) {
        return CargoSettlementResponse.builder()
                .id(settlement.getId())
                .deliveredValue(settlement.getDeliveredValue())
                .returnedValue(settlement.getReturnedValue())
                .coins(settlement.getCoins())
                .cash(settlement.getCash())
                .qr(settlement.getQr())
                .security(settlement.getSecurity())
                .total(settlement.getTotal())
                .settlementDate(settlement.getSettlementDate())
                .build();
    }

    private double sum(List<CargoLoad> loads, java.util.function.Function<CargoLoad, Double> extractor) {
        return loads.stream().mapToDouble(l -> extractor.apply(l) == null ? 0.0 : extractor.apply(l)).sum();
    }

    private double sumSettlements(List<CargoLoad> loads, java.util.function.Function<CargoSettlement, Double> extractor) {
        return loads.stream()
                .filter(l -> l.getSettlement() != null)
                .mapToDouble(l -> extractor.apply(l.getSettlement()) == null ? 0.0 : extractor.apply(l.getSettlement()))
                .sum();
    }
}
