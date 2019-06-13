//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------


package nl.colorize.util.rest;

/**
 * Used when dispatching requests to REST services to determine if the request
 * is authorized to access that service.
 */
public interface AuthorizationCheck {
    
    public static final AuthorizationCheck PUBLIC = new AuthorizationCheck() {
        public boolean isRequestAuthorized(RestRequest request, Rest serviceConfig) {
            return true;
        }
    };

    public boolean isRequestAuthorized(RestRequest request, Rest serviceConfig);
}
