package com.imeth.chronobank.web.rest;

import com.imeth.chronobank.common.constants.AppConstants;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Activator class that defines the base path for all REST endpoints.
 */
@ApplicationPath(AppConstants.API_BASE_PATH)
public class JaxRsActivator extends Application {
    // The path is defined by the ApplicationPath annotation
}