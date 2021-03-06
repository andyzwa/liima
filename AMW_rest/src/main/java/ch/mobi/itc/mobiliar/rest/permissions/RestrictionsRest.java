/*
 * AMW - Automated Middleware allows you to manage the configurations of
 * your Java EE applications on an unlimited number of different environments
 * with various versions, including the automated deployment of those apps.
 * Copyright (C) 2013-2016 by Puzzle ITC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.mobi.itc.mobiliar.rest.permissions;

import ch.mobi.itc.mobiliar.rest.dtos.PermissionDTO;
import ch.mobi.itc.mobiliar.rest.dtos.RestrictionDTO;
import ch.mobi.itc.mobiliar.rest.dtos.RestrictionsCreationDTO;
import ch.mobi.itc.mobiliar.rest.exceptions.ExceptionDto;
import ch.puzzle.itc.mobiliar.business.environment.boundary.ContextLocator;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.business.security.entity.PermissionEntity;
import ch.puzzle.itc.mobiliar.business.security.entity.RestrictionEntity;
import ch.puzzle.itc.mobiliar.business.security.entity.RoleEntity;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.*;

@Stateless
@Path("/permissions/restrictions")
@Api(value = "/permissions/restrictions", description = "Managing permissions")
public class RestrictionsRest {

    @Inject
    PermissionBoundary permissionBoundary;
    @Inject
    ContextLocator contextLocator;

    /**
     * Creates a new restriction and returns the newly created restriction.
     *
     * @param request containing a RestrictionDTO
     * @return the new RestrictionDTO
     **/
    @POST
    @ApiOperation(value = "Add a Restriction")
    public Response addRestriction(@ApiParam("Add a Restriction, either a role- or a userName must be set") RestrictionDTO request,
                                   @QueryParam("delegation") boolean delegation) {
        Integer id;
        if (request.getId() != null) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto("Id must be null")).build();
        }
        if (request.getPermission() == null) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto("Permission must not be null")).build();
        }
        try {
            id = permissionBoundary.createRestriction(request.getRoleName(), request.getUserName(), request.getPermission().getName(), request.getResourceGroupId(),
                    request.getResourceTypeName(), request.getResourceTypePermission(), request.getContextName(), request.getAction(), delegation);
        } catch (AMWException e) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto(e.getMessage())).build();
        }
        if (id == null) {
            return Response.status(PRECONDITION_FAILED).entity(new ExceptionDto("A similar permission already exists")).build();
        }
        return Response.status(CREATED).header("Location", "/permissions/restrictions/" + id).build();
    }

    /**
     * Creates new restrictions
     *
     * @param request containing a RestrictionsCreationDTO
     * @return the total count of created restrictions in the header
     */
    @POST
    @Path("/multi/")
    @ApiOperation(value = "Add a multiple Restrictions")
    public Response addRestriction(@ApiParam("Add multiple Restrictions, either a role- or one or more userNames must be set") RestrictionsCreationDTO request) {
        if (request.getPermissionNames().isEmpty()) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto("At least one Permission is required")).build();
        }
        int count;
        try {
            count = permissionBoundary.createMultipleRestrictions(request.getRoleName(), request.getUserNames(), request.getPermissionNames(), request.getResourceGroupIds(),
                    request.getResourceTypeNames(), request.getResourceTypePermission(), request.getContextNames(), request.getActions());
        } catch (AMWException e) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto(e.getMessage())).build();
        }
        return Response.status(CREATED).header("X-Total-Count", count).build();
    }

    /**
     * Find a Restriction by its id
     *
     * @param id
     * @return RestrictionDTO
     */
    @GET
    @Path("/{id : \\d+}")
    // support digit only
    @ApiOperation(value = "Get Restriction by id")
    public Response getRestriction(@ApiParam("Restriction ID") @PathParam("id") Integer id) {
        RestrictionEntity restriction = permissionBoundary.findRestriction(id);
        if (restriction == null) {
            return Response.status(NOT_FOUND).build();
        }
        return Response.status(OK).entity(new RestrictionDTO(restriction)).build();
    }

    /**
     * Find all Restrictions
     *
     * @return List<RestrictionDTO>
     */
    @GET
    @Path("/")
    @ApiOperation(value = "Get all Restrictions")
    public Response getAllRestriction() {
        List<RestrictionDTO> restrictions = new ArrayList<>();
        for (RestrictionEntity restrictionEntity : permissionBoundary.findAllRestrictions()) {
            restrictions.add(new RestrictionDTO(restrictionEntity));
        }
        return Response.status(OK).entity(restrictions).build();
    }

    /**
     * Update a Restriction
     * @param id
     */
    @PUT
    @Path("/{id : \\d+}")
    // support digit only
    @Produces("application/json")
    @ApiOperation(value = "Update a Restriction")
    public Response updateRestriction(@ApiParam("Restriction ID") @PathParam("id") Integer id, RestrictionDTO request) {
        boolean succcess;
        try {
            succcess = permissionBoundary.updateRestriction(id, request.getRoleName(), request.getUserName(), request.getPermission().getName(),
                    request.getResourceGroupId(), request.getResourceTypeName(), request.getResourceTypePermission(),
                    request.getContextName(), request.getAction());
        } catch (AMWException e) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto(e.getMessage())).build();
        }
        if (!succcess) {
            return Response.status(PRECONDITION_FAILED).entity(new ExceptionDto("A similar permission already exists")).build();
        }
        return Response.status(OK).build();
    }

    /**
     * Remove a Restriction
     * @param id
     */
    @DELETE
    @Path("/{id : \\d+}")
    // support digit only
    @ApiOperation(value = "Remove a Restriction")
    public Response deleteRestriction(@ApiParam("Restriction ID") @PathParam("id") Integer id) {
        try {
            permissionBoundary.removeRestriction(id);
        } catch (AMWException e) {
            return Response.status(NOT_FOUND).entity(new ExceptionDto(e.getMessage())).build();
        }
        return Response.status(NO_CONTENT).build();
    }

    /**
     * Find all Roles with their permissions/restrictions
     *
     * @return Map<RoleName, List<RestrictionDTO>>
     */
    @GET
    @Path("/roles/")
    @ApiOperation(value = "Get all Roles with restrictions")
    public Response getAllRoles() {
        Map<String, List<RestrictionDTO>> rolesMap = new HashMap<>();
        final Map<String, List<ch.puzzle.itc.mobiliar.business.security.entity.RestrictionDTO>> allRoles = permissionBoundary.getAllPermissions();
        // converting business RestrictionDTOs to rest RestrictionDTOs
        for (Map.Entry<String, List<ch.puzzle.itc.mobiliar.business.security.entity.RestrictionDTO>> roleRestrictionList : allRoles.entrySet()) {
            String roleName = roleRestrictionList.getKey();
            if (!rolesMap.containsKey(roleName)) {
                rolesMap.put(roleName, new ArrayList<RestrictionDTO>());
            }
            for (ch.puzzle.itc.mobiliar.business.security.entity.RestrictionDTO restrictionDTO : roleRestrictionList.getValue()) {
                rolesMap.get(roleName).add(new RestrictionDTO(restrictionDTO.getRestriction()));
            }
        }
        return Response.status(OK).entity(rolesMap).build();
    }

    /**
     * Find a specific Role with its Restrictions identified by RoleName
     *
     * @return List<RestrictionDTO>
     */
    @GET
    @Path("/roles/{roleName}")
    @ApiOperation(value = "Get all Restrictions assigned to a specific Role")
    public Response getRoleRestriction(@ApiParam("UserName") @PathParam("roleName") String roleName) {
        List<RestrictionDTO> restrictionList = new ArrayList<>();
        final List<RestrictionEntity> restrictions = permissionBoundary.getRestrictionsByRoleName(roleName);
        for (RestrictionEntity restriction : restrictions) {
            restrictionList.add(new RestrictionDTO(restriction));
        }
        return Response.status(OK).entity(restrictionList).build();
    }

    /**
     * Get all available RoleNames
     *
     * @return List<String>
     */
    @GET
    @Path("/roleNames/")
    @ApiOperation(value = "Get all available RoleNames")
    public Response getRoleNames() {
        List<String> roleNameList = new ArrayList<>();
        final List<RoleEntity> roles = permissionBoundary.getAllRoles();
        for (RoleEntity role : roles) {
            roleNameList.add(role.getName());
        }
        return Response.status(OK).entity(roleNameList).build();
    }

    /**
     * Get all available UserRestrictionNames
     *
     * @return List<String>
     */
    @GET
    @Path("/userRestrictionNames/")
    @ApiOperation(value = "Get all available userRestrictionNames")
    public Response getUserNames() {
        return Response.status(OK).entity(permissionBoundary.getAllUserRestrictionNames()).build();
    }

    /**
     * Get all available PermissionEnum values
     *
     * @return List<PermissionDTO>
     */
    @GET
    @Path("/permissionEnumValues/")
    @ApiOperation(value = "Get all available PermissionEnum values")
    public Response getPermissionEnumValues() {
        List<PermissionDTO> permissionNameList = new ArrayList<>();
        final List<PermissionEntity> permissions = permissionBoundary.getAllAvailablePermissions();
        for (PermissionEntity permission : permissions) {
            permissionNameList.add(new PermissionDTO(Permission.valueOf(permission.getValue())));
        }
        return Response.status(OK).entity(permissionNameList).build();
    }

    /**
     * Find all UserRestrictions with their Restrictions
     *
     * @return Map<UserRestriction, List<RestrictionDTO>>
     */
    @GET
    @Path("/users/")
    @ApiOperation(value = "Get all UserRestriction with their Restrictions")
    public Response getAllUsers() {
        Map<String, List<RestrictionDTO>> usersMap = new HashMap<>();
        final List<RestrictionEntity> allUserRestrictions = permissionBoundary.getAllUserRestriction();
        for (RestrictionEntity restriction : allUserRestrictions) {
            String userName = restriction.getUser().getName();
            if (!usersMap.containsKey(userName)) {
                usersMap.put(userName, new ArrayList<RestrictionDTO>());
            }
            usersMap.get(userName).add(new RestrictionDTO(restriction));
        }
        return Response.status(OK).entity(usersMap).build();
    }

    /**
     * Find a specific UserRestriction with its Restrictions identified by UserName
     *
     * @param userName
     * @return List<RestrictionDTO>
     */
    @GET
    @Path("/users/{userName}")
    @ApiOperation(value = "Get all Restrictions assigned to a specific UserRestriction")
    public Response getUserRestriction(@ApiParam("UserName") @PathParam("userName") String userName) {
        List<RestrictionDTO> restrictionList = new ArrayList<>();
        for (RestrictionEntity restriction : permissionBoundary.getRestrictionsByUserName(userName)) {
            restrictionList.add(new RestrictionDTO(restriction));
        }
        return Response.status(OK).entity(restrictionList).build();
    }

    /**
     * Find all Restrictions of the calling user
     *
     * @return List<RestrictionDTO>
     */
    @GET
    @Path("/ownRestrictions/")
    @ApiOperation(value = "Get all Restrictions assigned to a specific UserRestriction")
    public Response getCallerRestrictions() {
        List<RestrictionDTO> restrictionList = new ArrayList<>();
        for (RestrictionEntity restriction : permissionBoundary.getAllCallerRestrictions()) {
            restrictionList.add(new RestrictionDTO(restriction));
        }
        return Response.status(OK).entity(restrictionList).build();
    }

}
