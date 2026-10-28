package dev.quantumlab.simulation;

/** One point of a plotted series. Plain data so the UI layer can chart it however it likes. */
public record SeriesPoint(double x, double y) {}
