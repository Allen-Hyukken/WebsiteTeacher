package com.cerebro.finalproject.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "classroom")
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(name = "banner_path")
    private String bannerPath;

    @Lob
    @Column(name = "banner_image", columnDefinition = "LONGBLOB")
    private byte[] bannerImage;

    @Column(name = "banner_content_type", length = 50)
    private String bannerContentType;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "classroom_students",
            joinColumns = @JoinColumn(name = "classroom_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<User> students = new HashSet<>();

    @OneToMany(mappedBy = "classRoom", cascade = CascadeType.ALL)
    private Set<Quiz> quizzes = new HashSet<>();

    // Constructors
    public Classroom() {}

    public Classroom(String name, String code, User teacher) {
        this.name = name;
        this.code = code;
        this.teacher = teacher;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getBannerPath() { return bannerPath; }
    public void setBannerPath(String bannerPath) { this.bannerPath = bannerPath; }

    public byte[] getBannerImage() { return bannerImage; }
    public void setBannerImage(byte[] bannerImage) { this.bannerImage = bannerImage; }

    public String getBannerContentType() { return bannerContentType; }
    public void setBannerContentType(String bannerContentType) { this.bannerContentType = bannerContentType; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public Set<User> getStudents() { return students; }
    public void setStudents(Set<User> students) { this.students = students; }

    public Set<Quiz> getQuizzes() { return quizzes; }
    public void setQuizzes(Set<Quiz> quizzes) { this.quizzes = quizzes; }
}