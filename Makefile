APP_NAME = LibraryExtra
COMPOSE_FILE = docker-compose.yml

GREEN = \033[0;32m
YELLOW = \033[1;33m
RED = \033[0;31m
NC = \033[0m

.PHONY: help run restart rebuild stop clean

help:
	@echo "$(GREEN)📚 $(APP_NAME)$(NC)"
	@echo "$(YELLOW)make run     $(NC)- Compila e sobe tudo"
	@echo "$(YELLOW)make rebuild $(NC)- Recompila e rebuilda"
	@echo "$(YELLOW)make stop    $(NC)- Para tudo"
	@echo "$(YELLOW)make clean   $(NC)- Limpa tudo"

run:
	@echo "$(GREEN)🔨 Compilando...$(NC)"
	./mvnw clean package -DskipTests
	@echo "$(GREEN)🚀 Subindo serviços...$(NC)"
	docker-compose up -d
	@echo "$(GREEN)✅ Pronto!$(NC)"
	@echo "$(YELLOW)📱 App: http://localhost:8080$(NC)"
	@echo "$(YELLOW)🔧 pgAdmin: http://localhost:5050$(NC)"

rebuild: stop clean-network
	@echo "$(GREEN)🔨 Recompilando...$(NC)"
	./mvnw clean package -DskipTests
	docker-compose up -d --build

stop:
	docker-compose down

clean:
	docker-compose down -v --rmi all
	./mvnw clean

clean-network: ## Remove redes Docker conflitantes
	@echo "$(YELLOW)🔧 Removendo redes conflitantes...$(NC)"
	@docker network rm $(NETWORK_NAME) 2>/dev/null || true
	@docker network prune -f