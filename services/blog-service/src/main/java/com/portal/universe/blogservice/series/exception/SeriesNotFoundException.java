package com.portal.universe.blogservice.series.exception;

/**
 * 시리즈를 찾을 수 없을 때 발생하는 예외
 */
public class SeriesNotFoundException extends RuntimeException {
    public SeriesNotFoundException(String seriesId) {
        super("Series not found: " + seriesId);
    }
}