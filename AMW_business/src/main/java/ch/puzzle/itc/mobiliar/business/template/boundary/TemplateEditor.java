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

package ch.puzzle.itc.mobiliar.business.template.boundary;

import java.util.*;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import ch.puzzle.itc.mobiliar.business.database.entity.MyRevisionEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.*;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationContextEntity;
import ch.puzzle.itc.mobiliar.business.security.entity.Action;
import ch.puzzle.itc.mobiliar.business.template.entity.RevisionInformation;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.template.entity.TemplateDescriptorEntity;
import ch.puzzle.itc.mobiliar.common.exception.*;
import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import ch.puzzle.itc.mobiliar.business.environment.control.ContextDomainService;
import ch.puzzle.itc.mobiliar.business.template.control.FreemarkerSyntaxValidator;
import ch.puzzle.itc.mobiliar.business.security.interceptor.HasPermission;
import ch.puzzle.itc.mobiliar.business.security.interceptor.HasPermissionInterceptor;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationService;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.common.util.SystemCallTemplate;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
@Interceptors(HasPermissionInterceptor.class)
public class TemplateEditor {

	@Inject
	EntityManager entityManager;

	@Inject
	ContextDomainService contextService;

	@Inject
	ResourceRelationService relationService;

	@Inject
	PermissionService permissionService;

	@Inject
	FreemarkerSyntaxValidator freemarkerValidator;

	@Inject
	private Logger log;

	public TemplateDescriptorEntity getTemplateById(Integer templateId) {
		return entityManager.find(TemplateDescriptorEntity.class, templateId);
	}

	public TemplateDescriptorEntity getTemplateByIdAndRevision(Integer templateId, Number revisionId) {
		TemplateDescriptorEntity templateDescriptorEntity = AuditReaderFactory.get(entityManager).find(
				TemplateDescriptorEntity.class, templateId, revisionId);
	     //We have to ensure, that the target platforms are loaded. To make sure, that the compiler doesn't optimize the access to the target platforms away, we have to do this ugly hack.
	    	templateDescriptorEntity.getTargetPlatforms().size();
	    	return templateDescriptorEntity;
	}

	public List<RevisionInformation> getTemplateRevisions(Integer templateId) {
		List<RevisionInformation> result = new ArrayList<>();
		AuditReader reader = AuditReaderFactory.get(entityManager);
		List<Number> list = reader.getRevisions(TemplateDescriptorEntity.class, templateId);
		for (Number rev : list) {
			Date date = reader.getRevisionDate(rev);
			MyRevisionEntity myRev = entityManager.find(MyRevisionEntity.class, rev);
			result.add(new RevisionInformation(rev, date, myRev.getUsername()));
		}
		Collections.sort(result);
		return result;
	}

	public List<TemplateDescriptorEntity> loadTemplateDescriptors(HasContexts<?> hasContext) {
		hasContext = entityManager.find(hasContext.getClass(), hasContext.getId());
		List<? extends ContextDependency<?>> dependencies = hasContext.getContextsByLowestContext(contextService.getGlobalResourceContextEntity());
		List<TemplateDescriptorEntity> templates = new ArrayList<>();
		for (ContextDependency<?> dep : dependencies) {
			templates.addAll(dep.getTemplates());
		}
		Collections.sort(templates, new Comparator<TemplateDescriptorEntity>() {

			@Override
			public int compare(TemplateDescriptorEntity t1, TemplateDescriptorEntity t2) {
				if (t1 == null || t1.getName() == null) {
					return t2 == null || t2.getName() == null ? 0 : -1;
				}
				else {
					return t2 == null ? 1 : t1.getName().compareTo(t2.getName());
				}
			}
		});
		return templates;
	}

	public <T extends HasContexts<?>> void saveTemplateForRelation(
			TemplateDescriptorEntity template, Integer relationId, boolean resourceEdit)
					throws AMWException{
		HasContexts<?> resourceRelation = null;
		if (relationId != null) {
			if (resourceEdit) {
			     permissionService.checkPermissionAndFireException(Permission.RESOURCE_TEMPLATE, Action.UPDATE, "save resource templates");
				resourceRelation = relationService.getResourceRelation(relationId);
			}
			else {
			    permissionService.checkPermissionAndFireException(Permission.RESOURCETYPE_TEMPLATE, Action.UPDATE, "save resource type templates");
			    resourceRelation = relationService.getResourceTypeRelation(relationId);
			}
		}
		if (resourceRelation != null) {
			saveTemplate(template, resourceRelation);
		}
	}


	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	boolean hasTemplateWithSameName(TemplateDescriptorEntity template, HasContexts<?> hasContext) {
		for (ContextDependency<?> c : hasContext.getContextsByLowestContext(contextService
				.getGlobalResourceContextEntity())) {
			for (TemplateDescriptorEntity t : c.getTemplates()) {
				// If the template doesn't exist but has the same name, we return true
				if (!t.getId().equals(template.getId()) && t.getName().equals(template.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	@HasPermission(permission = Permission.RESOURCE_TEMPLATE, oneOfAction = {Action.UPDATE, Action.CREATE})
    public void saveTemplateForResource(TemplateDescriptorEntity template, Integer resourceId) throws AMWException {
		saveTemplate(template, entityManager.find(ResourceEntity.class, resourceId));
    }

    @HasPermission(permission = Permission.RESOURCETYPE_TEMPLATE, oneOfAction = {Action.UPDATE, Action.CREATE})
    public void saveTemplateForResourceType(TemplateDescriptorEntity template, Integer resourceTypeId) throws AMWException {
		saveTemplate(template, entityManager.find(ResourceTypeEntity.class, resourceTypeId));
    }

	public boolean hasPermissionToAddResourceTypeTemplate(Integer resourceTypeId, boolean testingMode) {
		ResourceTypeEntity type = entityManager.find(ResourceTypeEntity.class, resourceTypeId);
		return permissionService.hasPermissionToAddResourceTypeTemplate(type, testingMode);
	}

	public boolean hasPermissionToUpdateResourceTypeTemplate(Integer resourceTypeId, boolean testingMode) {
		ResourceTypeEntity type = entityManager.find(ResourceTypeEntity.class, resourceTypeId);
		return permissionService.hasPermissionToUpdateResourceTypeTemplate(type, testingMode);
	}

	public boolean hasPermissionToAddResourceTemplate(Integer resourceId, boolean testingMode) {
		ResourceEntity res = entityManager.find(ResourceEntity.class, resourceId);
		return permissionService.hasPermissionToAddResourceTemplate(res, testingMode);
	}

	public boolean hasPermissionToUpdateResourceTemplate(Integer resourceId, boolean testingMode) {
		ResourceEntity res = entityManager.find(ResourceEntity.class, resourceId);
		return permissionService.hasPermissionToUpdateResourceTemplate(res, testingMode);
	}

    void saveTemplate(TemplateDescriptorEntity template, HasContexts<?> hasContext) throws AMWException {
		if (StringUtils.isEmpty(template.getName())) {
			throw new AMWException("The template name must not be empty");
		}
		freemarkerValidator.validateFreemarkerSyntax(template.getFileContent());
		hasContext = entityManager.find(hasContext.getClass(), hasContext.getId());
		if (hasTemplateWithSameName(template, hasContext)) {
			throw new AMWException("The defined template name is already in use");
		}
		if (hasContext instanceof HasTypeContext
				&& hasTemplateWithSameName(template, ((HasTypeContext<?>) hasContext).getTypeContext())) {
			throw new AMWException("The defined template name is already in use");
		}
		ContextDependency<?> globalContext = hasContext.getOrCreateContext(contextService
				.getGlobalResourceContextEntity());
		if (template.getId() == null) {
			entityManager.persist(template);
			globalContext.addTemplate(template);
			entityManager.persist(globalContext);
		}
		else {
			entityManager.merge(template);
		}
	}

	/**
	 * @param templateId - the id of the template to be deleted.
	 *
	 * @throws ResourceTypeNotFoundException
	 * @throws TemplateNotDeletableException
	 */
	public void removeTemplate(Integer templateId) throws TemplateNotDeletableException {
		TemplateDescriptorEntity templateDescriptor = entityManager.find(TemplateDescriptorEntity.class,
				templateId);
		if (templateDescriptor != null && templateDescriptor.getName() != null
				&& SystemCallTemplate.getName().equals(templateDescriptor.getName())) {
			String message = SystemCallTemplate.getName() + " Template can't be deleted since it is a system template!";
			log.info(message);
			throw new TemplateNotDeletableException(message);
		}
		AbstractContext owner = getOwnerOfTemplate(templateDescriptor);
		if (owner != null) {
			if (owner instanceof ResourceContextEntity && !permissionService.hasPermission(Permission.RESOURCE_TEMPLATE, null,
					Action.DELETE, ((ResourceContextEntity) owner).getContextualizedObject().getResourceGroup(), null)) {
				throw new NotAuthorizedException("Not authorized to remove the template of a resource");
			} else if (owner instanceof ResourceRelationContextEntity && !permissionService.hasPermission(Permission.RESOURCE_TEMPLATE, null,
					Action.DELETE, ((ResourceRelationContextEntity) owner).getContextualizedObject().getMasterResource().getResourceGroup(), null)) {
				throw new NotAuthorizedException("Not authorized to remove the template of a resource");
			} else if (owner instanceof ResourceTypeContextEntity && !permissionService.hasPermission(Permission.RESOURCETYPE_TEMPLATE, null,
					Action.DELETE, null, ((ResourceTypeContextEntity) owner).getContextualizedObject())) {
				throw new NotAuthorizedException("Not authorized to remove the template of a resource type");
			}
			owner.removeTemplate(templateDescriptor);
		}
		entityManager.remove(templateDescriptor);
		log.info("Template " + templateId + " has been deleted successfully.");
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	AbstractContext getOwnerOfTemplate(TemplateDescriptorEntity templateDescriptor) {
		// ContextEntity
		AbstractContext c;
		c = (AbstractContext) getSingleObjectOrNull(entityManager.createQuery(
				"select distinct n from ContextEntity n where :templ member of n.templates")
				.setParameter("templ", templateDescriptor));
		if (c != null) {
			return c;
		}
		c = (AbstractContext) getSingleObjectOrNull(entityManager.createQuery(
				"select distinct n from ContextTypeEntity n where :templ member of n.templates")
				.setParameter("templ", templateDescriptor));
		if (c != null) {
			return c;
		}
		c = (AbstractContext) getSingleObjectOrNull(entityManager.createQuery(
				"select distinct n from ResourceContextEntity n where :templ member of n.templates")
				.setParameter("templ", templateDescriptor));
		if (c != null) {
			return c;
		}
		c = (AbstractContext) getSingleObjectOrNull(entityManager
				.createQuery(
						"select distinct n from ResourceRelationContextEntity n where :templ member of n.templates")
						.setParameter("templ", templateDescriptor));
		if (c != null) {
			return c;
		}
		c = (AbstractContext) getSingleObjectOrNull(entityManager
				.createQuery(
						"select distinct n from ResourceRelationTypeContextEntity n where :templ member of n.templates")
						.setParameter("templ", templateDescriptor));
		if (c != null) {
			return c;
		}
		c = (AbstractContext) getSingleObjectOrNull(entityManager.createQuery(
				"select distinct n from ResourceTypeContextEntity n where :templ member of n.templates")
				.setParameter("templ", templateDescriptor));
		if (c != null) {
			return c;
		}
		return null;

	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	Object getSingleObjectOrNull(Query q) {
		try {
			return q.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

}
