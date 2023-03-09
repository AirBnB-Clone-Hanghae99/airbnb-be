package com.example.airbnbpractice.repository;

import com.example.airbnbpractice.entity.House;
import com.example.airbnbpractice.entity.HouseWish;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HouseRepository extends JpaRepository<House, Long> {
    List<House> findAllByOwnerId(Long ownerId);

    List<House> findByWishHouses_UserId(Long userId);

    Optional<House> findHouseByIdAndOwnerId(Long houseId, Long userId);

    @Query("SELECT h FROM houses h WHERE (:adminDistrict IS NULL " +
            "OR :adminDistrict = '' OR h.adminDistrict = :adminDistrict) " +
            "AND (:peopleCount IS NULL OR h.maxPeople >= :peopleCount) " +
            "AND (:minPrice IS NULL OR :maxPrice IS NULL " +
            "OR (h.pricePerDay >= :minPrice AND h.pricePerDay <= :maxPrice)) " +
            "AND (:startDate IS NULL OR :endDate IS NULL OR NOT EXISTS (" +
            "SELECT r FROM reservations r WHERE r.house.id = h.id AND (" +
            "(:startDate >= r.checkin AND :startDate <= r.checkout) OR " +
            "(:endDate >= r.checkin AND :endDate <= r.checkout) OR " +
            "(:startDate <= r.checkin AND :endDate >= r.checkout))))")
    @EntityGraph(attributePaths = {
            "owner", "houseImages", "wishHouses", "houseTags", "houseTags.tag", "houseTags.tag.tagType"
    })
    List<House> searchHomes(String adminDistrict, Integer peopleCount,
                            Integer minPrice, Integer maxPrice,
                            LocalDate startDate, LocalDate endDate, Pageable pageable);



    @Query("select CASE WHEN COUNT(h.id) > 0 THEN true ELSE false END from houses h WHERE h.id = :houseId AND " +
            "NOT EXISTS (SELECT r FROM reservations r WHERE r.house.id = h.id " +
            "AND ((:checkIn >= r.checkin AND :checkIn <= r.checkout) " +
            "OR (:checkOut >= r.checkin AND :checkOut <= r.checkout) " +
            "OR (:checkIn <= r.checkin AND :checkOut >= r.checkout)))")
    Boolean checkReservedHouse(Long houseId, LocalDate checkIn, LocalDate checkOut);
}
