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

// TODO notes stack
// Do not update note on cancel
// Show alternatives in UI, override opponent's move.

const hflexStyle = {display: "flex", flexDirection: "row"}
const vflexStyle = {display: "flex", flexDirection: "column"}

const deepCopy = o => JSON.parse(JSON.stringify(o));

var handle = null; // Will point to top level React component for app

const syncAppState = () => {
  handle.setState(deepCopy(appState));
};

const initialFrame = {
  fen: initialFen,
  note: ""
};

var appState = {
  frameStack: [initialFrame],
  frameIdx: 0,
  variationComplete: false,
  color: "w",
  selectedSquare: null,
  flipBoard: false,
};

const getFrame = () => {
  let { frameStack,
	frameIdx } = appState;
  return frameStack[frameIdx];
};

const getFen = () => getFrame().fen;

const getNote = () => getFrame().note;

const setNote = (note) => {
  getFrame().note = note;
}

const getMaxFrameIdx = () => {
  return appState.frameStack.length - 1;
};

const viewingLatestFrame = () => {
  return (appState.frameIdx == getMaxFrameIdx());
};

const playerHasTurn = () => {
  let { color } = appState;
  let activeColor = getActiveColor(getFen());
  return (color == activeColor);
};

const Button = (props) => {
  return React.createElement(
    "button",
    {
      onClick: props.onClick,
      style: {
	maxWidth: "150px"
      }
    },
    props.text
  );
};

class CommentWidget extends React.Component {
  render() {
    let { notesText } = this.props;
    return React.createElement(
      "div",
      {
	style: {
	  ...vflexStyle,
	  width: "400px",
	  maxWidth: "400px",
	  minHeight: "300px",
	  height: "300px"
	}
      },
      React.createElement(
      	"p",
      	null,
      	notesText
      ),
      Button({
	text: "Update notes",
	onClick: () => {
	  let newNote = prompt("Update note:", notesText);
	  setNote(newNote);
	  syncAppState();
	  fetch('note', {
	    method: 'POST',
	    headers: { 'Content-Type': 'application/json;charset=utf-8' },
	    body: JSON.stringify({
	      fen: getFen(),
	      note: newNote
	    })
	  })
	}
      })
    );
  };
}

class Page extends React.Component {
  constructor(props) {
    super(props);
    this.state = props;
    handle = this;
  };
  
  render() {
    let { frameStack,
	  frameIdx,
	  color,
	  flipBoard,
	  selectedSquare,
	} = this.state;
    let frame = frameStack[frameIdx];
    let { fen,
	  note } = frame;
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
      ),
      React.createElement(
	CommentWidget,
	{
	  notesText: note
	}
      )
    );
  };
}

const resetBoard = () => {
  appState.frameStack = [initialFrame];
  appState.frameIdx = 0;
  appState.variationComplete = false;
  appState.selectedSquare = null;
  syncAppState();
  if (appState.color == "b") {
    opponentPlaysFirstMove();
  }
};

const flipBoard = () => {
  appState.flipBoard = !appState.flipBoard;
  syncAppState();
};

// The `Board` React element expects its `clickPieceFn` to take a
// color and square, thus we take both args here despite that `color`
// is not used.
const clickPiece = (color, square) => {
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

const move = (fen, note) => {
  appState.frameStack.push({fen: fen, note: note});
  appState.frameIdx = getMaxFrameIdx();
  appState.selectedSquare = null;
  syncAppState();
};

const tryMove = (fromSquare, toSquare) => {
  let body = {
    fen: getFen(),
    from: fromSquare,
    to: toSquare,
  };
  // TODO handle promotions
  fetch('move', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json;charset=utf-8' },
    body: JSON.stringify(body)
  }).then(response => response.json())
    .then(responseJSON => {
      //console.log(
      // `Sent move to server. Response: ${JSON.stringify(responseJSON)}`
      //);
      let { correct, fen, end, note } = responseJSON;
      if (correct) {
	console.log("The move was correct");
	move(fen, note);
	if (end == 'true') {
	  console.log("End of variation");
	  appState.variationComplete = true;
	} else {
	  console.log("Getting the opponent move");
	  setTimeout(
	    opponentMove,
	    500
	  );
	}
      } else {
	appState.selectedSquare = null;
	syncAppState();
      }
    })
};

const opponentMove = () => {
  fetch(
    'opponent-move',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json;charset=utf-8' },
      body: JSON.stringify({fen: getFen()})
    })
    .then(response => response.json())
    .then(responseJSON => {
      let { fen, note } = responseJSON;
      move(fen, note);
    })
};

const clickSquare = (clickedSquare) => {
  console.log(`Clicked square=${clickedSquare}`
	      + `movingIsAllowed=${movingIsAllowed()}`);
  if (movingIsAllowed()) {
    let { selectedSquare } = appState;
    tryMove(selectedSquare, clickedSquare);
  }
}

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
    } else {
      resetBoard();
    }
    break;
  }
});

const opponentPlaysFirstMove = () => {
  setTimeout(
    opponentMove,
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

