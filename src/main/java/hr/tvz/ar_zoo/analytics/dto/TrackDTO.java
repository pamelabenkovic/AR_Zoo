package hr.tvz.ar_zoo.analytics.dto;

public record TrackDTO(
        String type,
        String page,
        String modelId,
        String modelName,
        String clientId
) {}
