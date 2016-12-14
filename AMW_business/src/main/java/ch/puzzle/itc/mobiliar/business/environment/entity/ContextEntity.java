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

package ch.puzzle.itc.mobiliar.business.environment.entity;

import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeContextEntity;
import ch.puzzle.itc.mobiliar.business.security.entity.PermissionEntity;
import ch.puzzle.itc.mobiliar.business.shakedown.entity.ShakedownTestEntity;
import ch.puzzle.itc.mobiliar.common.util.ContextNames;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

import static javax.persistence.CascadeType.PERSIST;

/**
 * Entity implementation class for Entity: ResourceContext
 */
@Entity
@Audited
@Table(name = "TAMW_context", uniqueConstraints = {
        @UniqueConstraint(name = "IU_CONTEXT", columnNames = {"name"})})
@NamedQuery(name = ContextEntity.LOAD_CONTEXT_BY_NAME_QUERY_NAME, query = "select c from ContextEntity c where c.name=:name", cacheable = true)
@AssociationOverrides({
        @AssociationOverride(name = "properties", joinTable = @JoinTable(name = "TAMW_context_prop")),
        @AssociationOverride(name = "propertyDescriptors", joinTable = @JoinTable(name = "TAMW_context_propDesc")),
        @AssociationOverride(name = "templates", joinTable = @JoinTable(name = "TAMW_context_tmplDesc"))
})
public class ContextEntity extends AbstractContext implements Serializable {
    public static final String LOAD_CONTEXT_BY_NAME_QUERY_NAME = "loadContextByName";


    @Getter
    @Setter
    @ManyToOne(optional = true)
    private ContextEntity parent;

    @Getter
    @Setter
    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE)
    @org.hibernate.annotations.ForeignKey(name = "FK_M4D9672RME6C3JI7SAWH0NFMM")
    @AuditMappedBy(mappedBy = "parent")
    @OrderBy(value = "name")
    private Set<ContextEntity> children;

    @Getter
    @Setter
    @ManyToOne(cascade = {PERSIST})
    @org.hibernate.annotations.ForeignKey(name = "FK_SOO4P4WNMVSPP6KS0LK3R6MVN")
    private ContextTypeEntity contextType;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "context")
    @AuditMappedBy(mappedBy = "context")
    private Set<ResourceContextEntity> resourceContextEntities;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "context")
    @AuditMappedBy(mappedBy = "context")
    private Set<ResourceRelationContextEntity> resourceRelationContextEntities;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "context")
    @AuditMappedBy(mappedBy = "context")
    private Set<ResourceRelationTypeContextEntity> resourceRelationTypeContextEntities;

    @Getter
    @Setter
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "context")
    @AuditMappedBy(mappedBy = "context")
    private Set<ResourceTypeContextEntity> resourceTypeContextEntities;

    @Getter
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "context")
    @NotAudited
    private Set<PermissionEntity> permissions;

    @Getter
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "context")
    @NotAudited
    private Set<DeploymentEntity> deploys;

    @Getter
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "context")
    @AuditMappedBy(mappedBy = "context")
    @NotAudited
    private Set<ShakedownTestEntity> shakedownTests;

    @Getter
    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private String name;

    public ContextEntity() {
        super();
    }


    public void remove() {
        getParent().getChildren().remove(this);
    }

    @Override
    public String toString() {
        return "ContextEntity [name=" + name + "]";
    }

    public boolean isEnvironment() {
        return ContextNames.ENV.name().equals(getContextType().getName());
    }

    public boolean isGlobal() {
        return ContextNames.GLOBAL.name().equals(getContextType().getName());
    }


}
