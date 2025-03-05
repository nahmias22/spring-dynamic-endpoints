package com.example.demo.model;

public class TestDTO {

    private String string;
    private Long number;

    public TestDTO() {
    }

    public TestDTO(String string, Long number) {
        this.string = string;
        this.number = number;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }
}
