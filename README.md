# ♟️ Jogo de Xadrez em Java

Este é um projeto de **xadrez desenvolvido em Java** com interface gráfica em **Swing**.  
Ele foi criado como estudo acadêmico, mas implementa diversas regras oficiais e recursos adicionais, incluindo **IA** e **controle de tempo**.

---

## 🚀 Funcionalidades

- ✅ Regras completas do xadrez:
  - Movimentos corretos de todas as peças.
  - Detecção de **xeque**.
  - Detecção de **xeque-mate**.
  - Detecção de **empate (xeque-pato/stalemate)**.
  - Promoção de peão.
- ✅ Captura de peças exibida nas laterais do tabuleiro.
- ✅ Jogador pode escolher jogar com **peças brancas** ou **pretas**.
- ✅ **Modo contra IA**:
- ✅ Jogar contra outro jogador **ou contra a IA**
- ✅ Escolher a cor das peças (Brancas ou Pretas)
- ✅ **IA básica**, que faz jogadas automáticas para o adversário
- ✅ Botão para **reiniciar o jogo**.

---

## 📂 Estrutura do Projeto
jogo-xadrez-java/
├── src/
│ ├── controller/
│ │ └── Game.java
│ ├── model/
│ │ ├── board/
│ │ │ ├── Board.java
│ │ │ └── Position.java
│ │ └── pieces/
│ │ ├── Piece.java
│ │ ├── Pawn.java
│ │ ├── Rook.java
│ │ ├── Bishop.java
│ │ ├── Knight.java
│ │ ├── Queen.java
│ │ └── King.java
│ └── view/
│ └── ChessGUI.java
├── README.md
└── .gitignore

───

## 🖥️ Tecnologias Utilizadas

- **Java SE**
- **Swing (javax.swing)** para a interface gráfica.
- **Collections e estruturas de dados** para manipulação do tabuleiro.
- **Programação Orientada a Objetos (POO)**:
  - Classes organizadas em `model`, `controller` e `view`.

---


---

## ▶️ Como Executar

1. Clone este repositório:
   ```bash
   git clone https://github.com/seu-usuario/jogo-xadrez-java.git

2. Compile o projeto (dentro da pasta raiz):
   javac -d bin src/**/*.java

3. Execute o jogo:
java -cp bin view.ChessGUI


Créditos

Projeto inspirado no código base da professora Thayse utilizado na disciplina de POO.
Implementações, melhorias e novas funcionalidades desenvolvidas por Marina Queiroz:
Adição de IA básica
Escolha de jogar com brancas ou pretas
Captura de peças exibidas lateralmente
Timer de jogadas
Reinício de jogo
Detecção de xeque, xeque-mate e xeque-pato
Outras melhorias estruturais



