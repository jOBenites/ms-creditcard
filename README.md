# ms-creditcard

Microservicio de gestión de tarjetas de crédito del sistema bancario. Expone CRUD completo, emisión de tarjetas personales y empresariales con línea de crédito, consume `bank.customer.created` para validar clientes sin REST entre microservicios, y publica `bank.creditcard.issued` al emitir una tarjeta.

## Requisitos

- Java 17
- Maven 3.9+
- MongoDB (puerto 27017)
- Kafka (puerto 9092)
- Config Server corriendo en puerto 8888

## Variables de entorno

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `server.port` | `8084` | Puerto del servicio |
| `spring.config.import` | `optional:configserver:http://localhost:8888` | URL del Config Server |
| `spring.data.mongodb.uri` | `mongodb://...creditcard_db` | URI de MongoDB (fallback) |
| `spring.kafka.consumer.group-id` | `ms-creditcard` | Grupo consumidor de Kafka |

## Levantar

```bash
mvn spring-boot:run
```

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/credit-cards` | Emitir tarjeta |
| GET | `/credit-cards/{id}` | Buscar por ID |
| GET | `/credit-cards` | Listar todas |
| PUT | `/credit-cards/{id}` | Actualizar linea de credito |
| DELETE | `/credit-cards/{id}` | Eliminar |

## Base de datos

- **Database:** `creditcard_db`
- **Colecciones:** `credit_card`, `customer_view`

## Eventos

| Topic | Dirección | Trigger | Payload |
|-------|-----------|---------|---------|
| `bank.customer.created` | Consume | ms-customer crea un cliente | `customerId`, `customerType`, `profile`, `documentNumber`, `occurredAt` |
| `bank.creditcard.issued` | Produce | Al emitir una tarjeta | `cardId`, `customerId`, `cardType`, `creditLimit`, `occurredAt` |

## Verificar

```bash
mvn verify
```

Ejecuta tests unitarios, Checkstyle y genera reporte JaCoCo.
