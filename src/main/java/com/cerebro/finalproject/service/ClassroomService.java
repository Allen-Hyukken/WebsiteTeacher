package com.cerebro.finalproject.service;

import com.cerebro.finalproject.model.Classroom;
import com.cerebro.finalproject.model.User;
import com.cerebro.finalproject.repository.ClassroomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class ClassroomService {

    @Autowired
    private ClassroomRepository classroomRepository;

    private static final String DEFAULT_BANNER_PATH = "static/images/default-class.jpg";

    public Classroom createClass(String name, User teacher, MultipartFile banner) {
        Classroom classroom = new Classroom();
        classroom.setName(name);
        classroom.setTeacher(teacher);
        classroom.setCode(generateUniqueCode());

        if (banner != null && !banner.isEmpty()) {
            try {
                classroom.setBannerImage(banner.getBytes());
                classroom.setBannerContentType(banner.getContentType());
            } catch (IOException e) {
                e.printStackTrace();
                setDefaultBanner(classroom);
            }
        } else {
            setDefaultBanner(classroom);
        }

        return classroomRepository.save(classroom);
    }

    private void setDefaultBanner(Classroom classroom) {
        try {
            ClassPathResource defaultBanner = new ClassPathResource(DEFAULT_BANNER_PATH);
            InputStream inputStream = defaultBanner.getInputStream();
            byte[] defaultImageBytes = StreamUtils.copyToByteArray(inputStream);
            classroom.setBannerImage(defaultImageBytes);
            classroom.setBannerContentType("image/jpeg");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Optional<Classroom> findById(Long id) {
        return classroomRepository.findById(id);
    }

    public List<Classroom> findByTeacherId(Long teacherId) {
        return classroomRepository.findByTeacherId(teacherId);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = generateRandomCode(6);
        } while (classroomRepository.findByCode(code).isPresent());
        return code;
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}