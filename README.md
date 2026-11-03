# GoTogether

Projeto acadêmico desenvolvido para o PBL da disciplina de Redes de Computadores, explorando assuntos
como sockets TCP/IP, APIs REST, arquitetura cliente-servidor e arquitetura peer-to-peer.

O projeto simula uma plataforma de caronas na qual motoristas podem cadastrar viagens
com rotas entre cidades e passageiros podem pesquisar itinerários, realizando reservas 
de forma concorrente e atomicamente segura.

## Como rodar a aplicação

### Configurar o .env da aplicação:

O arquivo .env.example exemplifica a configuração do ambiente da aplicação. 
Está pré-definida as portas de cada servidor, basta informar o IP de cada um.

Em um terminal (executar o servidor):

```
docker compose up --build
```

Em outro terminal (executar o cliente):

```
docker compose -f compose-client.yaml up --build
```

### Executar em máquinas diferentes:

Escolha o servidor e o cliente que quer executar com o comando:

Para servidor (substitua server1 para o serviço que quer executar):

```
docker compose up --build server1
```

Para cliente (substitua client1 para o serviço que quer executar)

```
docker compose -f compose-client.yaml up --build client1
```

## Protocolo de comunicação

### (Sockets TCP/IP)

As mensagens são transmitidas em JSON.

Cada requisição possui a seguinte estrutura:

```
{ 
    "type": "TIPO_DA_OPERACAO", 
    "data": {} 
}    
```

As respostas possuem:

```
{ 
    "status": "OK", 
    "message": "Mensagem",
    "data": {} 
}    
```

## Operações do protocolo

| Tipo               | Usuário    | Descrição                        | Principais dados                    |
|--------------------|------------|----------------------------------|-------------------------------------|
| `LOGIN`              | Ambos      | Realiza autenticação do usuário  | E-mail e senha                      |
| `REGISTER`           | Ambos      | Registra uma nova conta          | Dados do usuário e tipo             |
| `PUBLISH_RIDE`       | Motorista  | Cadastra uma nova carona         | Rota, data, horário, vagas e preços |
| `LIST_RIDES`         | Motorista  | Lista as caronas do motorista    | —                                   |
| `DELETE_RIDE`        | Motorista  | Cancela uma carona própria       | ID da carona                        |
| `SEARCH_RIDES`       | Passageiro | Pesquisa itinerários disponíveis | Origem, destino e data              |
| `RESERVE_ITINERARY`  | Passageiro | Reserva um itinerário            | Trechos do itinerário               |
| `LIST_RESERVATIONS`  | Passageiro | Lista as reservas do passageiro  | —                                   |
| `DELETE_RESERVATION` | Passageiro | Cancela uma reserva              | ID da reserva                       |

## Exemplo de comunicação

Exemplo de requisição de login:

```
{
    "type": "LOGIN",
    "data": {
        "email": "usuario@email.com",
        "password": "123456"
    }
}
```

Exemplo de resposta:

```
{
    "status": "SUCCESS",
    "message": "Login realizado com sucesso.",
    "data": {}
}
```

### Descrição API REST

Consulte a descrição da API:



