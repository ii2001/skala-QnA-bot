package com.skala.qna.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.skala.qna.organization.OrganizationService;

@Component
public class AdminBootstrap {

	private final OrganizationService organization;
	private final String email;
	private final String password;
	private final String name;

	public AdminBootstrap(OrganizationService organization,
			@Value("${ADMIN_BOOTSTRAP_EMAIL:}") String email,
			@Value("${ADMIN_BOOTSTRAP_PASSWORD:}") String password,
			@Value("${ADMIN_BOOTSTRAP_NAME:SKALA Admin}") String name) {
		this.organization = organization;
		this.email = email == null ? "" : email.trim();
		this.password = password == null ? "" : password;
		this.name = name == null || name.isBlank() ? "SKALA Admin" : name.trim();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void bootstrap() {
		if (!email.isBlank() && !password.isBlank()) {
			organization.bootstrapAdmin(name, email, password);
		}
	}
}
