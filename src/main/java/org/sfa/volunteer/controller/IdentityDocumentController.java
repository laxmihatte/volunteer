package org.sfa.volunteer.controller;

import org.sfa.volunteer.exception.ForbiddenException;
import org.sfa.volunteer.service.IdentityDocumentStorageService;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.JwtClaims;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/0.0.1/volunteers")
public class IdentityDocumentController {

    private final IdentityDocumentStorageService storage;
    private final UserService userService;
    private final ResponseBuilder responseBuilder;

    @Autowired
    public IdentityDocumentController(IdentityDocumentStorageService storage,
                                      UserService userService,
                                      ResponseBuilder responseBuilder) {
        this.storage = storage;
        this.userService = userService;
        this.responseBuilder = responseBuilder;
    }

    private void authorizeUser(JwtClaims claims, String targetUserId) {
    	if (isAdmin(claims.groups())) return;

    	if (claims.email() == null || claims.email().isBlank()) {
        throw new ForbiddenException("JWT email missing");
        }

    	String callerSid;
    	try {
            callerSid = userService.getUserIdByEmailForAuth(claims.email());
    	} catch (Exception e) {
            throw new ForbiddenException("JWT user not mapped to DB user");
        }
        if (callerSid == null || callerSid.isBlank()) {
            throw new ForbiddenException("JWT user not mapped to DB user");
        }
        if (!callerSid.equals(targetUserId)) {
            throw new ForbiddenException("Not allowed");
        }
    }

   private boolean isAdmin(String groups) {
      if (groups == null || groups.isBlank()) return false;
      for (String g : groups.split(",")) {
        if (g.trim().equalsIgnoreCase("admin")) return true;
    }
    return false;
   }
}