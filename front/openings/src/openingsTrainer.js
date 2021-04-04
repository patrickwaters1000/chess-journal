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
  variationComplete: false,
  color: "w",
  selectedSquare: null,
  flipBoard: false,
  notesText: ""
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

/*
class Note extends React.Component {
  constructor(props) {
    super(props);
    this.state = { text: this.props.text };
  }
  
  shouldComponentUpdate(nextProps) {
    if (nextProps.text !== this.state.text) {
      this.setState({ text: nextProps.text });
      return true;
    }
    return false;
    //return true;
  }
  
  updateText(e) {
    event.preventDefault();
    this.setState({ text: nextProps.text });
    appState.text = e.target.value;
    syncAppState();
  }

  render() {
    return React.createElement(
      "textarea",
      { onChange: this.updateText },
      this.props.text
    );
  }
};
*/
/*
(e) => {
	  console.log(`Updating note to ${e.target.value}`);
	  appState.notesText = e.target.value;
	  syncAppState();
	},
*/

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
      //React.createElement(
      //	"textarea",
      //	{
      //	  onChange: (e) => {
      //	    console.log(`Updating note to ${e.target.value}`);
      //	    appState.notesText = e.target.value;
      //	    syncAppState();
      //	  },
      //	},
      //	""
      //),
      //React.createElement(
      //	Note,
      //	{ text: notesText } 
      //),
      Button({
	text: "Update notes",
	onClick: () => {
	  let newNote = prompt("Update note:", notesText);
	  appState.notesText = newNote;
	  syncAppState();
	  fetch('note', {
	    method: 'POST',
	    headers: { 'Content-Type': 'application/json;charset=utf-8' },
	    body: JSON.stringify({note: newNote})
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
    let { fenStack,
	  frameIdx,
	  color,
	  flipBoard,
	  selectedSquare,
	  notesText
	} = this.state;
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
      ),
      React.createElement(
	CommentWidget,
	{
	  notesText: notesText
	}
      )
    );
  };
}

const resetBoard = () => {
  fetch('reset', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json;charset=utf-8' },
  }).then(() => {
    appState.fenStack = [initialFen];
    appState.frameIdx = 0;
    appState.variationComplete = false;
    appState.selectedSquare = null;
    syncAppState();
    if (appState.color == "b") {
      opponentPlaysFirstMove();
    }
  });
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

const move = (fen) => {
  appState.fenStack.push(fen);
  appState.frameIdx = getMaxFrameIdx();
  appState.selectedSquare = null;
  syncAppState();
};

const tryMove = (fromSquare, toSquare) => {
  let body = {
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
      let { correct, fen, end } = responseJSON;
      if (correct) {
	console.log("The move was correct");
	move(fen);
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
    })
    .then(response => response.json())
    .then(responseJSON => {
      let { fen, note } = responseJSON;
      appState.notesText = note;
      move(fen);
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

