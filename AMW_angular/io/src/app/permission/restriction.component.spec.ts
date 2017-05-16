import { inject, TestBed } from '@angular/core/testing';
import { RestrictionComponent } from './restriction.component';
import { Restriction } from './restriction';
import { Environment } from '../deployment/environment';
import { Resource } from '../resource/resource';

describe('RestrictionComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEach(() => TestBed.configureTestingModule({
    providers: [
      RestrictionComponent
    ]
  }));

  it('should preSelect the right Environment on ngOnChanges',
    inject([RestrictionComponent], (restrictionComponent: RestrictionComponent) => {
      // given
      let emptyEnvironment: Environment[] = [ { id: null, name: null, parent: 'All', selected: false } ];
      let devEnvironments: Environment[] = [ { id: 1, name: 'B', parent: 'Dev', selected: false },
        { id: 2, name: 'C', parent: 'Dev', selected: false } ];
      restrictionComponent.groupedEnvironments =  { 'All': emptyEnvironment, 'Dev': devEnvironments };
      restrictionComponent.restriction = <Restriction> { contextName: 'C' };
      // when
      restrictionComponent.ngOnChanges();
      // then
      expect(restrictionComponent.groupedEnvironments['All'][0]['selected']).toBeFalsy();
      expect(restrictionComponent.groupedEnvironments['Dev'][0]['selected']).toBeFalsy();
      expect(restrictionComponent.groupedEnvironments['Dev'][1]['selected']).toBeTruthy();
  }));

  it('should return all parent Environment names',
    inject([RestrictionComponent], (restrictionComponent: RestrictionComponent) => {
      // given
      let emptyEnvironment: Environment[] = [ { id: null, name: null, parent: 'All', selected: false } ];
      let devEnvironments: Environment[] = [ { id: 1, name: 'B', parent: 'Dev', selected: false },
        { id: 2, name: 'C', parent: 'Dev', selected: false } ];
      let testEnvironments: Environment[] = [ { id: 11, name: 'T', parent: 'Test', selected: false },
        { id: 12, name: 'S', parent: 'Test', selected: false } ];
      restrictionComponent.groupedEnvironments =  { 'All': emptyEnvironment, 'Dev': devEnvironments, 'Test': testEnvironments };
      // when
      let groups: string[] = restrictionComponent.getEnvironmentGroups();
      // then
      expect(groups[0]).toBe('All');
      expect(groups[1]).toBe('Dev');
      expect(groups[2]).toBe('Test');
  }));

  it('should return false if ResourceGroup has a name which is not available',
    inject([RestrictionComponent], (restrictionComponent: RestrictionComponent) => {
      // given
      restrictionComponent.resourceGroups = [ <Resource> { id: 21, name: 'Test' } ];
      restrictionComponent.resourceGroup = <Resource> { id: null, name: 'West' };
      // when then
      expect(restrictionComponent.checkGroup()).toBeFalsy();
  }));

  it('should return true if ResourceGroup has a name which is available',
    inject([RestrictionComponent], (restrictionComponent: RestrictionComponent) => {
      // given
      restrictionComponent.resourceGroups = [ <Resource> { id: 21, name: 'Test' }, <Resource> { id: 42, name: 'Rest' } ];
      restrictionComponent.resourceGroup = <Resource> { id: null, name: 'rest' };
      restrictionComponent.restriction = <Restriction> {};
      // when then
      expect(restrictionComponent.checkGroup()).toBeTruthy();
  }));

  it('should return invalid if ResourceType is not available',
    inject([RestrictionComponent], (restrictionComponent: RestrictionComponent) => {
      // given
      restrictionComponent.resourceTypes = [ { id: 1, name: 'APP'}, { id: 2, name: 'APPSERVER' } ];
      restrictionComponent.restriction = <Restriction> { resourceTypeName: 'INVALID' };
      // when then
      expect(restrictionComponent.isValidForm()).toBeFalsy();
  }));

  it('should return valid if ResourceType is available',
    inject([RestrictionComponent], (restrictionComponent: RestrictionComponent) => {
      // given
      restrictionComponent.resourceTypes = [ { id: 1, name: 'APP'}, { id: 2, name: 'APPSERVER' } ];
      restrictionComponent.restriction = <Restriction> { resourceTypeName: 'APPSERVER' };
      // when then
      expect(restrictionComponent.isValidForm()).toBeTruthy();
  }));

  it('should set ResourceTypeName to null if its value is empty',
    inject([RestrictionComponent], (restrictionComponent: RestrictionComponent) => {
      // given
      restrictionComponent.restriction = <Restriction> { resourceTypeName: '' };
      // when
      restrictionComponent.persistRestriction();
      // then
      expect(restrictionComponent.restriction.resourceTypeName).toBeNull();
  }));

});
