import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/services/auth.service';

describe('App', () => {
  const render = async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient()],
    }).compileComponents();

    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture;
  };

  it('muestra la marca de la comunidad', async () => {
    const fixture = await render();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Nakama Hub');
  });

  it('sin sesión ofrece entrar y crear cuenta, y no enseña el muro', async () => {
    const fixture = await render();
    const html = fixture.nativeElement as HTMLElement;

    expect(html.querySelector('a[href="/entrar"]')).not.toBeNull();
    expect(html.querySelector('a[href="/registro"]')).not.toBeNull();
    expect(html.querySelector('a[href="/muro"]')).toBeNull();
  });

  it('con sesión enseña el muro y el acceso a publicar', async () => {
    const fixture = await render();
    TestBed.inject(AuthService).startSession({
      id: 1,
      username: 'luffy',
      email: 'luffy@nakamahub.dev',
      accessToken: 'token',
      refreshToken: 'refresco',
      expiresIn: 3600,
    });
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('a[href="/muro"]')).not.toBeNull();
    expect(html.querySelector('a[href="/publicar"]')).not.toBeNull();
    expect(html.querySelector('a[href="/entrar"]')).toBeNull();
  });
});
