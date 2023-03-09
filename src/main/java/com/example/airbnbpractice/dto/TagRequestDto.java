package com.example.airbnbpractice.dto;

import com.example.airbnbpractice.entity.House;
import com.example.airbnbpractice.entity.TagType;
import com.example.airbnbpractice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;

@Getter
@AllArgsConstructor
public class TagRequestDto {
    private String name;
    private Long tagTypeId;
    private MultipartFile imageFile;
}
