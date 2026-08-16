package com.rectificadora.gestion.repository;

import com.rectificadora.gestion.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
}
