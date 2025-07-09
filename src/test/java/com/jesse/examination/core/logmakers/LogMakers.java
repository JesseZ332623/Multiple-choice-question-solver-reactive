package com.jesse.examination.core.logmakers;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class LogMakers
{
    public static final Marker EMAIL_SENDER
        = MarkerFactory.getMarker("EMAIL_SENDER");

    public static final Marker REDIS_BASIC
        = MarkerFactory.getMarker("REDIS_BASIC");

    public static final Marker FILE_OPERATOR
        = MarkerFactory.getMarker("FILE_OPERATOR");
}
