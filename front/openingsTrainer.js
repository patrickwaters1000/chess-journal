import React, { Component } from "react";
import ReactDOM from "react-dom";
import Board from "./Board.js";
import { getActiveColor,
         initialFen,
         getSquareMap,
         isPromotion } from "./chessUtils.js";
import { parseFen,
	 getPieces,
	 getPiecesFromFen } from "./Fen.js";
import { fenToCorrectMoves,
         fenToOpponentMoves,
         fenXMoveToFen } from "./openingsData.js";
import { shuffle } from "lodash";

const hflexStyle = {display: "flex", flexDirection: "row"}
const vflexStyle = {display: "flex", flexDirection: "column"}

const deepCopy = o => JSON.parse(JSON.stringify(o));

var handle = null; // Will point to top level React component for app

const syncAppState = () => {
  handle.setState(deepCopy(appState));
};

var appState = {
  fenStack: [initialFen],
  frameIdx: 0,
  color: "b",
  selectedSquare: null,
  flipBoard: false
};

const getFen = () => {
  let { fenStack,
	frameIdx } = appState;
  return fenStack[frameIdx];
};

const getMaxFrameIdx = () => {
  return appState.fenStack.length - 1;
};

const viewingLatestFrame = () => {
  return (appState.frameIdx == getMaxFrameIdx());
};

const playerHasTurn = () => {
  let { color } = appState;
  let activeColor = getActiveColor(getFen());
  return (color == activeColor);
};

class Page extends React.Component {
  constructor(props) {
    super(props);
    this.state = props;
    handle = this;
  };
  
  render() {
    let { fenStack,
	  frameIdx,
	  color,
	  flipBoard,
	  selectedSquare } = this.state;
    let fen = fenStack[frameIdx];
    return React.createElement(
      "div",
      { style: hflexStyle },
      React.createElement(
	Board,
	{
	  pieces: getPieces(parseFen(fen)),
	  flipBoard: flipBoard,
	  // TODO replace selectedPieceSquare => selectedSquare in React component.
	  selectedPieceSquare: selectedSquare,
	  clickPieceFn: clickPiece,
	  clickSquareFn: clickSquare
	}
      )
    );
  };
}

const resetBoard = () => {
  appState.fenStack = [initialFen];
  appState.frameIdx = 0;
  appState.selectedSquare = null;
  syncAppState();
  if (appState.color == "b") {
    opponentPlaysFirstMove();
  }
}

const flipBoard = () => {
  appState.flipBoard = !appState.flipBoard;
  syncAppState();
};

// The `Board` React element expects its `clickPieceFn` to take a
// color and square, thus we take both args here despite that `color`
// is not used.
const clickPiece = (color, square) => {
  //console.log(`Clicking ${JSON.stringify(square)}; hasTurn=${playerHasTurn()}; viewingLatestFrame=${viewingLatestFrame()}; frameIdx=${appState.frameIdx}; maxFrameIdx=${getMaxFrameIdx()}; numFrames=${appState.fenStack.length}`);
  if (playerHasTurn()
      && viewingLatestFrame()) {
    if (appState.selectedSquare != square) {
      appState.selectedSquare = square;
    } else {
      appState.selectedSquare = null;
    }
    syncAppState();
  };
};

const movingIsAllowed = () => {
  return (playerHasTurn()
	  && viewingLatestFrame()
	  && appState.selectedSquare != null);
};

const moveIsCorrect = (clickedSquare) => {
  let correctMoves = fenToCorrectMoves[getFen()];
  let stringifiedCorrectMoves = correctMoves.map(JSON.stringify);
  /*if (!correctMoves) {
    console.log(`No correct moves? Can not find ${getFen()} in ${JSON.stringify(Object.keys(fenToCorrectMoves))}`);
  }*/
  let move = JSON.stringify([appState.selectedSquare, clickedSquare]);
  /*if (stringifiedCorrectMoves.indexOf(move) == -1) {
    console.log(`Incorrect move. Can not find ${move} in ${JSON.stringify(correctMoves)}`);
  }*/
  return (stringifiedCorrectMoves.indexOf(move) != -1);
};

const applyMove = (move) => {
  let oldFen = getFen();
  // let key = `[${JSON.stringify(oldFen)} ${JSON.stringify(move)}]`;
  let key = JSON.stringify([oldFen, move]); 
  let newFen = fenXMoveToFen[key];
  if (newFen == null) {
    console.log(`Can not find ${key} in ${JSON.stringify(Object.keys(fenXMoveToFen))}`);
  }
  appState.fenStack.push(newFen);
  let frameIdx = getMaxFrameIdx();
  appState.frameIdx = frameIdx;
  syncAppState();
};

const lineContinues = () => {
  let fen = getFen();
  //let moves = (playerHasTurn()
  //	       ? fenToCorrectMoves[fen]
  //	       : fenToOpponentMoves[fen]);
  let moves = fenToOpponentMoves[fen];
  if (moves != null) {
    console.log(`Line continues because ${fen} was found in fenToOpponentMoves; the moves are ${JSON.stringify(fenToOpponentMoves[fen])}`);
  }
  return (moves != null);
};

const chooseOpponentMove = () => {
  let fen = getFen();
  let opponentMoves = fenToOpponentMoves[fen];
  return shuffle(opponentMoves)[0];
};

const clickSquare = (clickedSquare) => {
  console.log(`Clicked square=${clickedSquare}`
	      + `movingIsAllowed=${movingIsAllowed()}`
	      + `moveIsCorect=${moveIsCorrect(clickedSquare)}`);
  if (movingIsAllowed()) {
    if (moveIsCorrect(clickedSquare)) {
      let { selectedSquare } = appState;
      let playerMove = [selectedSquare, clickedSquare];
      gameLoop(playerMove);
    } else {
      appState.selectedSquare = null;
      syncAppState();
    }
  }
}

const gameLoop = (playerMove) => {
  applyMove(playerMove);
  setTimeout(
    () => {
      if (lineContinues()) {
	console.log(`Line continues!`);
	let opponentMove = chooseOpponentMove();
	applyMove(opponentMove);
      } else {
	resetBoard();
      }
    },
    500
  );
};

document.addEventListener("keydown", (e) => {
  switch (e.code) {
  case "ArrowLeft":
    if (appState.frameIdx > 0) {
      appState.frameIdx -= 1;
      syncAppState();
    }
    break;
  case "ArrowRight":
    if (appState.frameIdx < getMaxFrameIdx()) {
      appState.frameIdx += 1;
      syncAppState();
    }
    break;
  }
});

const opponentPlaysFirstMove = () => {
  setTimeout(
    () => { applyMove(chooseOpponentMove()); },
    500
  );
};

window.addEventListener("DOMContentLoaded", () => {
  const div = document.getElementById("main");
  const page = React.createElement(Page, appState);
  ReactDOM.render(page, div);
  if (appState.color == "b") {
    opponentPlaysFirstMove();
  }
});
