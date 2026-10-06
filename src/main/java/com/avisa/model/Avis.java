package com.avisa.model;

import java.time.LocalDate;

public record Avis(String id, String missatge, LocalDate data) {
}
