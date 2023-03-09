package com.example.airbnbpractice.repository;

import com.example.airbnbpractice.entity.House;
import com.example.airbnbpractice.entity.TagType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TagTypeRepository extends JpaRepository<TagType, Long> {
//    List<TagType> findByTags_HouseTags_House(House house);


    @Query("SELECT distinct tt FROM tagTypes tt " +
            "JOIN FETCH tt.tags t " +
            "JOIN FETCH t.houseTags ht " +
            "WHERE ht.house.id = :houseId")
    List<TagType> findTagTypeByHouseIdJPQL(Long houseId);
}
