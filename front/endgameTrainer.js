import React, { Component } from "react";
import ReactDOM from "react-dom";
import Board from "./Board.js";
import { drills } from "./endgameDrills.js";
import { shuffle } from "lodash";

const hflexStyle = {display: "flex", flexDirection: "row"}
const vflexStyle = {display: "flex", flexDirection: "column"}

var handle = null;

const deepCopy = o => JSON.parse(JSON.stringify(o));

const syncAppState = () => {
  handle.setState(deepCopy(appState));
};

const getActiveColor = (fen) => {
  let words = fen.split(" ");
  switch (words[1]) {
  case "w":
    return "w";
    break;
  case "b":
    return "b";
    break;
  }
};

// Nice improvements I could make
// 1 Handle checkmate / stalemate correctly on server side
// 3 Show moves played
// 4 Allow drills with Black to play

//const shuffledDrills = shuffle(drills);
const shuffledDrills = drills;

var appState = {
  commentBoxHidden: true,
  drillIdx: 0,
  fen: shuffledDrills[0].fen,
  color: "w", // color that user is playing as; CURRENTLY NOT USED
  selectedPieceSquare: null,
  flipBoard: false // CURRENTLY ALWAYS false
};

const getDrillFEN = (idx) => {
  return shuffledDrills[idx].fen;
};

const getDrillComment = (idx) => {
  return shuffledDrills[idx].comment;
};

const resetDrill = () => {
  appState.drillIdx %= shuffledDrills.length;
  appState.fen = getDrillFEN(appState.drillIdx);
  appState.selectedPieceSquare = null;
  syncAppState();
}

const nextDrill = () => {
  appState.drillIdx += 1;
  resetDrill();
}

// Parses a FEN substring representing one rank of the board.
// Example: "3P2q1" => ["","","","P","","","q",""]
const parseRank = rank => {
  let a = [];
  rank.split("").forEach(c => {
    if (c >= '1' && c <= '8') {
      let n = parseInt(c);
      for (let i = 0; i < n; i++) {
	a.push("");
      }
    } else {
      a.push(c)
    }
  });
  return a;
}

const parseFEN = (fen) => {
  let board = fen.split(" ")[0];
  return board.split("/").map(parseRank);
};

const getPieces = (board) => {
  let pieces = [];
  board.forEach((rank, r) => {
    rank.forEach((x, f) => {
      if (x != "") {
	pieces.push({pieceType: x, rank: 7-r, file: f});
      }
    });
  });
  return pieces;
};

const getSquareMap = (squareStr) => {
  let file = {
    "A": 0, "B": 1, "C": 2, "D": 3, "E": 4, "F": 5, "G": 6, "H": 7
  }[squareStr.charAt(0)];
  let rank = 8 - parseInt(squareStr.charAt(1));
  return { rank: rank, file: file };
};

const isPromotion = (fromSquare) => {
  let board = parseFEN(appState.fen);
  let { rank, file } = getSquareMap(fromSquare)
  let piece = board[rank][file]
  if ((piece == "p" && rank == 6)
      || (piece == "P" && rank == 1)) {
    return true;
  }
  return false;
}

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

const toggleCommentBox = () => {
  appState.commentBoxHidden = !appState.commentBoxHidden;
  syncAppState();
};

class CommentWidget extends React.Component {
  render() {
    let { drillIdx,
	  commentBoxHidden } = this.props;
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
	// For some reason that I don't understand, the component
	// won't update if it's a "textarea" instead of a paragraph.
	"p",
	null,
	(commentBoxHidden
	 ? ""
	 : getDrillComment(drillIdx))
      ),
      Button({
	text: (commentBoxHidden
	       ? "Show comments"
	       : "Hide comments"),
	onClick: toggleCommentBox
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
    let s = this.state;
    return React.createElement(
      "div",
      { style: hflexStyle },
      React.createElement(
	Board,
	{
	  pieces: getPieces(parseFEN(s.fen)),
	  flipBoard: s.flipBoard,
	  selectedPieceSquare: s.selectedPieceSquare,
	  clickPieceFn: clickPiece,
	  clickSquareFn: clickSquare
	}
      ),
      React.createElement(
	"div",
	{
	  height: "75px",
	  width: "200px",
	  style: vflexStyle
	},
	Button({text: "Next drill",
		onClick: nextDrill}),
        Button({text: "Reset drill",
		onClick: resetDrill}),
	Button({text: "Get FEN",
		onClick: () => { confirm(appState.fen); }}),
	React.createElement(
	  CommentWidget,
	  {
	    drillIdx: s.drillIdx,
	    commentBoxHidden: s.commentBoxHidden
	  }
	)
      )
    );
  };
}

const flipBoard = () => {
  appState.flipBoard = !appState.flipBoard;
  syncAppState();
};

const clickPiece = (color, square) => {
  let active_color = getActiveColor(appState.fen);
  console.log(`Clicking piece, active color = ${active_color}`);
  if (color == active_color) {
    if (appState.selectedPieceSquare != square) {
      appState.selectedPieceSquare = square;
    } else {
      appState.selectedPieceSquare = null;
    }
    syncAppState();
  }
};

const getEngineMove = () => {
  console.log("Requesting engine move from server");
  fetch('engine', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json;charset=utf-8' },
    body: JSON.stringify({fen: appState.fen})
  }).then(response => response.json())
    .then(responseJSON => {
      console.log(
	`Engine move response: ${JSON.stringify(responseJSON)}`
      );
      appState.fen = responseJSON.fen;
      appState.selectedPieceSquare = null;
      syncAppState();
    });
};

const tryMove = (fromSquare, toSquare) => {
  let body = {
    from: fromSquare,
    to: toSquare,
    fen: appState.fen
  };
  if (isPromotion(fromSquare)) {
    body.promote = "Q"
  }
  fetch('move', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json;charset=utf-8' },
    body: JSON.stringify(body)
  }).then(response => response.json())
    .then(responseJSON => {
      console.log(
	`Sent move to server. Response: ${JSON.stringify(responseJSON)}`
      );
      appState.fen = responseJSON.fen;
      appState.selectedPieceSquare = null;
      syncAppState();
    })
    .then(getEngineMove);
};

const clickSquare = (toSquare) => {
  let fromSquare = appState.selectedPieceSquare;
  if (appState.selectedPieceSquare != null) {
    tryMove(fromSquare, toSquare);
  }
}

/*
document.addEventListener("keydown", (e) => {
  switch (e.code) {
  case "ArrowLeft":
    prevMove();
    break;
  case "ArrowRight":
    nextMove();
    break;
  }
});
*/

window.addEventListener("DOMContentLoaded", () => {
  const div = document.getElementById("main");
  const page = React.createElement(Page, appState);
  ReactDOM.render(page, div);
});
