# GoTogether

Projeto acadêmico desenvolvido para o PBL da disciplina de Redes de Computadores, utilizando 
sockets TCP/IP, concorrência e arquitetura cliente-servidor.

O GoTogether é um sistema de compartilhamento de caronas intermunicipais e interestaduais 
desenvovlido em Java utilizando comunicação TCP/IP por sockets.

O projeto simula uma plataforma de caronas na qual motoristas podem cadastrar viagens
com rotas entre cidades e passageiros podem pesquisar itinerários, realizando reservas 
de forma concorrente e atomicamente segura.

## Tecnologias

- Java 26
- Maven
- TCP/IP Sockets
- Jackson
- Lombok
- JUnit
- Docker

## Como rodar a aplicação

Em um terminal (executar o servidor):

```
mvn clean package
docker compose up --build
```

Em outro terminal (executar o cliente):

```
mvn clean package
docker compose -f compose-client.yaml up --build
```

### Executar em máquinas diferentes:

Após executar o servidor, é necessário descobrir o endereço IP da máquina do servidor:

```
ipconfig
```

Na máquina do cliente, o endereço do servidor é configurado no arquivo `compose-client.yaml`:

```
environment:
    SERVER_HOST: 192.168.0.25
    SERVER_PORT: 5000
```

Após isso, basta executar o cliente:

```
mvn clean package
docker compose -f compose-client.yaml up --build
```

## Arquitetura

O sistema possui uma arquitetura cliente-servidor. Cada conexão estabelecida cria uma
instância de um `ClienteHandler`, que é responsável por receber e processar as mensagens daquele cliente.

Os serviços e repositórios são compartilhados entre as conexões do servidor, permitindo que
diferentes clientes acessem e alterem o mesmo estado.

### Modelo de dados

#### Usuário

Os usuários possuem um identificador, dados de autenticação e um tipo: Motorista ou Passageiro.

#### Carona

Campos: 
- identificador;
- identificador do motorista;
- rota;
- data;
- horário de partida;
- trechos da rota.

Exemplo:

```
Feira de Santana → Salvador → Aracaju
```

é representada por:

```
Trecho 1:
Feira de Santana → Salvador

Trecho 2:
Salvador → Aracaju
```

Cada trecho possui sua própria quantidade de vagas disponíveis e preço.

## Comunicação

A comunicação entre clientes e servidor utiliza TCP/IP através de sockets Java.

Cada cliente estabelece uma conexão TCP com o servidor. O servidor aceita conexões
e cria uma thread independente para cada cliente.

A thread criada permanece responsável pela comunicação com aquele cliente durante toda a sessão.

Quando o cliente encerra a conexão, o servidor detecta o encerramento e libera os recursos
associados à sessão.

## Protocolo de comunicação

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

## Encapsulamento e validação

No cliente, os objetos são convertidos para JSON. A mensagem é enviada através do socket TCP.

No servidor, a mensagem é lida e convertida de JSON para o objeto de requisição.

Após o parsing, o servidor verifica se a requisição pode ser processada. Caso, por exemplo, os dados
sejam inválidos, o servidor envia uma resposta com status de erro, explicando o erro ao cliente.

## Busca de itinerários

O sistema utiliza uma busca em profundidade para encontrar caminhos entre a origem e o destino.
Isso permite encontrar itinerários com múltiplos trechos distintos.

## Reserva e Concorrência

A reserva precisa garantir que duas requisições concorrentes não vendam a mesma vaga.

Para isso, o serviço de reserva utiliza travas associadas aos trechos. Cada trecho possui uma trava própria.

As travas garantem que a reserva será feita apenas por um cliente de cada vez.

Antes de realizar a reserva, os trechos são ordenados e suas respectivas travas são adquiridas nessa ordem.

A ordenação garante uma ordem para aquisição das travas, evitando situações de deadlocks.

Testes de concorrência foram criados para testar a segurança do sistema quando múltiplos clientes reservam a mesma vaga.
Foram utilizadas 10 threads diferentes para simular a condição de corrida. Os testes apontaram resultados positivos.

## Atomicidade da reserva

Uma reserva pode envolver múltiplos trechos.

Todos os trechos são bloqueados antes da confirmação da reserva.

O sistema verifica a disponibilidade de todos antes de reservar um trecho.

Dessa forma, se qualquer um dos trechos estiver sem vagas no momento da reserva, a reserva não é realizada.

As travas são liberadas independentemente se foi possível realizar a reserva ou não, garantindo que uma
exceção ou encerramento da operação não deixe os trechos permanentemente bloqueados.


## Interação

A interação dos dois tipos de usuário se dá por meio do terminal. O usuário recebe mensagens via terminal e interage
enviando números que correspondem a cada operação no sistema.

## Confiabilidade

O servidor realiza validações das requisições recebidas e retorna respostas de erro quando uma operação
não pode ser executada.

Exemplos:
- credenciais inválidas;
- dados obrigatórios ausentes;
- carona inexistente;
- reserva inexistente;
- trecho inexistente;
- ausência de vagas;
- operação incompatível.

## Testes

Foram realizados testes automatizados para verificar a corretude do sistema e seu comportamento com múltiplos clientes simultâneos.

- Testes funcionais: verificam operações como publicação, busca, reserva e cancelamento.
- Testes de concorrência: simulam vários passageiros tentando realizar reservas ao mesmo tempo. A simulação é feita utilizando
diferentes threads para representar os clientes conectados simultaneamente.
- Consistência: verificam se as vagas não ficam negativas e se uma mesma vaga não é vendida para mais de um passageiro.

## Emulação

O Docker é utilizado para executar os componentes, simulando um ambiente distribuído.

A utilização de contêineres no Docker permite:
- reproduzir o mesmo ambiente em diferentes máquinas, pois facilita a configuração do ambiente;
- isolar servidor e cliente;
- executar múltiplas instâncias da aplicação;

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

