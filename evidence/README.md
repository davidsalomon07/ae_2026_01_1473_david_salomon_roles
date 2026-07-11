# evidence/

Coloca aquí (todas tomadas desde la **consola web de AWS** y Postman):

- `01_user_pool.png` — resumen de **tu** User Pool con el **User Pool ID**.
![alt text](image.png)
- `02_groups.png` — pestaña **Groups** con `ADMIN` y `USER`.
![alt text](image-1.png)
- `03_membership_admin.png` — usuario `admin_parking` mostrando su grupo `ADMIN`.
![alt text](image-2.png)
- `04_membership_user.png` — usuario `user_parking` mostrando su grupo `USER`.
![alt text](image-3.png)
- `05_app_client_domain.png` — App client + dominio **por defecto** de Cognito.
![alt text](image-4.png)
![alt text](image-5.png)
- `06_jwt_admin.png` / `07_jwt_user.png` — jwt.io con el claim `cognito:groups`.
![alt text](image-6.png)
![alt text](image-7.png)
- `08_postman_available_200.png` — `GET /parking-spaces/available` sin token.
![alt text](image-8.png)
- `09_postman_create_401_403_201.png` — `POST /parking-spaces` sin token / USER / ADMIN.
![alt text](image-9.png)
![alt text](image-10.png)
![alt text](image-11.png)
- `10_postman_entry_403_201.png` — `POST /tickets/entry` con ADMIN / USER.
![alt text](image-12.png)
![alt text](image-13.png)
- `coverage.png` — Run with Coverage del `TicketService` al 100%.
![alt text](image-14.png)
- `parking-roles.postman_collection.json` — colección exportada.
