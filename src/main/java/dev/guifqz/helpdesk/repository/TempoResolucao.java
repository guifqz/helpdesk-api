package dev.guifqz.helpdesk.repository;

import java.time.Instant;

public record TempoResolucao(Instant criadoEm, Instant resolvidoEm) {
}
