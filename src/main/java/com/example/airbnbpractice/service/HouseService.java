package com.example.airbnbpractice.service;

import com.example.airbnbpractice.common.CustomClientException;
import com.example.airbnbpractice.common.dto.ErrorMessage;
import com.example.airbnbpractice.common.dto.ResponseDto;
import com.example.airbnbpractice.common.s3.S3Service;
import com.example.airbnbpractice.common.security.UserDetailsImpl;
import com.example.airbnbpractice.dto.CheckResponseDto;
import com.example.airbnbpractice.dto.HouseRequestDto;
import com.example.airbnbpractice.dto.HouseResponseDto;
import com.example.airbnbpractice.entity.*;
import com.example.airbnbpractice.repository.HouseRepository;
import com.example.airbnbpractice.repository.HouseWishRepository;
import com.example.airbnbpractice.repository.TagRepository;
import com.example.airbnbpractice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseService {
    private final HouseRepository houseRepository;
    private final HouseWishRepository houseWishRepository;
    private final TagRepository tagRepository;
    private final S3Service s3Service;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<HouseResponseDto.HouseRes> getHouses(
            String adminDistrict, Integer peopleCount,
            Integer minPrice, Integer maxPrice,
            LocalDate startDate, LocalDate endDate,
             Integer page, Integer size, String sortBy,
            Boolean isAsc, Long userId
    ) {


        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "id");
        Pageable pageable = PageRequest.of(page, size, sort);

        List<House> houses = houseRepository.searchHomes(adminDistrict, peopleCount,
                minPrice, maxPrice,
                startDate, endDate, pageable);

        return houses.stream().map(HouseResponseDto.HouseRes::of).toList();
    }

    @Transactional
    public HouseResponseDto.HouseRes addHouse(HouseRequestDto.HouseAdd dto, User user) {

        List<Tag> tags = tagRepository.findByIdIn(dto.getTagIds());


        String thumbnailImageURL = null;

        if (dto.getThumbnailImage() != null) {
            thumbnailImageURL = s3Service.uploadSingle(dto.getThumbnailImage());
        }

        List<String> houseImageURLs = new ArrayList<>();

        if (dto.getHouseImages() != null && !dto.getHouseImages().isEmpty()) {
            houseImageURLs = s3Service.upload(dto.getHouseImages());
        }



        House house = House.builder().thumbnailUrl(thumbnailImageURL).dto(dto).ownerId(user.getId()).build();

        for (String houseImageURL : houseImageURLs) {
            house.addHouseImage(HouseImage.builder().house(house).imageURL(houseImageURL).build());
        }

        for (Tag tag : tags) {
            house.addHouseTag(HouseTag.builder().house(house).tag(tag).build());
        }

        house = houseRepository.save(house);

        return HouseResponseDto.HouseRes.noOwnerOf(house);
    }

    @Transactional
    public HouseResponseDto.HouseRes updateHouse(Long houseId, HouseRequestDto.HouseUpdate dto, User user) {

        House house = houseRepository.findById(houseId).orElseThrow(
                () -> CustomClientException.of(ErrorMessage.NO_HOUSE)
        );

        if (!house.getOwnerId().equals(user.getId())) {
            throw CustomClientException.of(ErrorMessage.EDIT_REJECT);
        }

        house.update(dto);

        // 썸네일 등록
        if (dto.getThumbnailImage() != null) {
            String thumbnailImageURL = s3Service.uploadSingle(dto.getThumbnailImage());
            house.setThumbnailUrl(thumbnailImageURL);
        }


        // 이미지 목록 등록
        if (dto.getHouseImages() != null && !dto.getHouseImages().isEmpty()) {
            List<String> houseImageURLs = s3Service.upload(dto.getHouseImages());

            house.getHouseImages().clear();
            for (String houseImageURL : houseImageURLs) {
                house.addHouseImage(HouseImage.builder().house(house).imageURL(houseImageURL).build());
            }
        }

        //태그 등록
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findByIdIn(dto.getTagIds());
            if (!tags.isEmpty()) {
                house.getHouseTags().clear();
                for (Tag tag : tags) {
                    house.addHouseTag(HouseTag.builder().house(house).tag(tag).build());
                }
            }

        }

        return HouseResponseDto.HouseRes.noOwnerOf(house);
    }

    @Transactional
    public Boolean toggleWish(Long houseId, User user) {
        House house = houseRepository.findById(houseId).orElseThrow(() -> new CustomClientException(ErrorMessage.NO_HOUSE));

        if (houseWishRepository.findByHouseIdAndUserId(houseId, user.getId()).isPresent()) {
            houseWishRepository.deleteHouseWishByHouseIdAndUserId(user.getId(), house.getId());
            return false;
        }
        HouseWish houseWish = new HouseWish(user, house);
        houseWishRepository.save(houseWish);

        return true;
    }

    @Transactional(readOnly = true)
    public List<HouseResponseDto.HouseRes> registration(UserDetailsImpl userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(
                () -> CustomClientException.of(ErrorMessage.NO_USER)
        );
        List<House> houses = houseRepository.findAllByOwnerId(user.getId());
        if(houses == null){
            throw CustomClientException.of(ErrorMessage.NO_HOUSE);
        }
        List<HouseResponseDto.HouseRes> dtoList = new ArrayList<>();
        for (House house : houses) {
            HouseResponseDto.HouseRes houseGet = HouseResponseDto.HouseRes.of(house);
            dtoList.add(houseGet);
        }
        return dtoList;
    }
}

