//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------


package nl.colorize.util.rest;

/**
 * Called before a request to a REST service is dispatched, to determine if
 * the request is authorized to access that service.
 */
@FunctionalInterface
public interface AuthorizationCheck {

    public static final AuthorizationCheck PUBLIC = (request, service) -> true;
    
    public boolean isRequestAuthorized(RestRequest request, Rest service);
}
