import {ComponentFixture, TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GuardianList} from './guardian-list';
import {GuardianService} from '../../data-access/guardian.service';
import {provideRouter} from '@angular/router';

describe('GuardianList', () => {
  let component: GuardianList;
  let fixture: ComponentFixture<GuardianList>;

  const guardianServiceMock = {
    findAll: vi.fn()
  };

  const emptyPage = {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    guardianServiceMock.findAll.mockReturnValue(of(emptyPage));

    await TestBed.configureTestingModule({
      imports: [GuardianList],
      providers: [
        provideRouter([]),
        {
          provide: GuardianService,
          useValue: guardianServiceMock
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GuardianList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the first page', () => {
    expect(guardianServiceMock.findAll).toHaveBeenCalledWith(0, 20);
    expect(component.guardians()).toEqual([]);
    expect(component.currentPage()).toBe(0);
    expect(component.totalElements()).toBe(0);
  });
});
