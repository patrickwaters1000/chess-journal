import React, { Component } from "react";
import ReactDOM from "react-dom";
import Board from "./Board.js";
import { getPiecesFromFen } from "./Fen.js";

const hflexStyle = {display: "flex", flexDirection: "row"}
const vflexStyle = {display: "flex", flexDirection: "column"}

var handle = null;

const deepCopy = o => JSON.parse(JSON.stringify(o));

const syncAppState = () => {
  handle.setState(deepCopy(appState));
};

var exampleDrill = {
  name: "Sicilian defence",
  description: "Demonstrates the move defining the Sicilian defence",
  tags: ["Openings", "Sicilian"],
  frames: [
    {
      fen0: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      fen1: "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
      fen2: "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
      active_color: "b",
      answer_move: {from: "C7", to: "C5"},
      question_comment: "What is the first move of the Sicilian defense?",
      answer_comment: "Yes! This is a popular opening for Black."
    },
    {
      fen0: "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
      fen1: "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
      fen2: "rnbqkbnr/pp2pppp/3p4/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 3",
      active_color: "b",
      answer_move: {from: "D7", to: "D6"},
      question_comment: "What is the second move of the Sicilian defense (Najdorf variation)?",
      answer_comment: "Correct! This move discourages White from playing e5."
    }
  ]
};

const nullDrill = {
  name: "",
  description: "",
  tags: [],
  frames: [{
    fen0: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    active_color: "w"
  }]
};

const appState = {
  pieces: [],
  drill: nullDrill,
  drillFrameIdx: null,
  drillFrameStage: null,
  selectedPieceSquare: null,
  active_color: null,
  flipBoard: null,
};

const clickPiece = (color, square) => {
  console.log("Clicked a piece!");
  let s = appState;
  if (s.drillFrameStage == "main"
      && color == s.active_color) {
    console.log(`Tryin to do something with square = ${square}`);
    if (s.selectedPieceSquare != square) {
      s.selectedPieceSquare = square;
    } else {
      s.selectedPieceSquare = null;
    }
    syncAppState();
  } else {
    console.log(`frame stage = ${s.drillFrameStage}`);
    console.log(`piece color = ${color}`);
    console.log(`active color = ${s.active_color}`);
  }
};

const clickSquare = (toSquare) => {
  let s = appState;
  let fromSquare = s.selectedPieceSquare;
  if (s.drillFrameStage == "main"
      && s.selectedPieceSquare != null) {
    console.log(`Trying to move from ${fromSquare} to ${toSquare}`);
    tryMove(fromSquare, toSquare);
  } else {
    console.log(`frame stage = ${s.drillFrameStage}`);
    console.log(`to square = ${toSquare}`);
    console.log(`selected square = ${s.selectedPieceSquare}`);
  }
}

const tryMove = (fromSquare, toSquare) => {
  let s = appState;
  let f = s.drill.frames[s.drillFrameIdx];
  let a = f.answer_move;
  if (a.from == fromSquare
      && a.to == toSquare) {
    drillFramePostStage();
  } else {
    console.log("Wrong answer!");
    s.selectedPieceSquare = null;
    syncAppState();
  }
};

const beginDrill = (drill) => {
  appState.drill = drill;
  appState.drillFrameIdx = 0;
  drillFramePreStage();
};

const advanceDrillFrame = () => {
  let s = appState;
  let maxFrameIdx = s.drill.frames.length - 1;
  if (s.drillFrameIdx < maxFrameIdx
      && s.drillFrameStage == "post") {
    s.drillFrameIdx += 1;
    drillFramePreStage();
  }
};

const drillFramePreStage = () => {
  let s = appState;
  let f = s.drill.frames[s.drillFrameIdx];
  s.drillFrameStage = "pre";
  s.selectedPieceSquare = null;
  s.flipBoard = (f.active_color == "b");
  s.active_color = null;
  s.pieces = getPiecesFromFen(f.fen0);
  syncAppState();
  setTimeout(
    drillFrameMainStage,
    1000
  );
};

const drillFrameMainStage = () => {
  let s = appState;
  let f = s.drill.frames[s.drillFrameIdx];
  s.drillFrameStage = "main";
  s.selectedPieceSquare = null;
  s.flipBoard = (f.active_color == "b");
  s.active_color = f.active_color;
  s.pieces = getPiecesFromFen(f.fen1);
  syncAppState();
};

const drillFramePostStage = () => {
  let s = appState;
  let f = s.drill.frames[s.drillFrameIdx];
  s.drillFrameStage = "post";
  s.selectedPieceSquare = null;
  s.flipBoard = (f.active_color == "b");
  s.active_color = null;
  s.pieces = getPiecesFromFen(f.fen2);
  syncAppState();
};


const logState = (msg) => {
  console.log(msg, JSON.stringify(appState));
};

function isFunction(functionToCheck) {
 return functionToCheck && {}.toString.call(functionToCheck) === '[object Function]';
}

class Page extends React.Component {
  constructor(props) {
    super(props);
    this.state = props;
    handle = this;
  };
  
  render() {
    let s = this.state;
    let f = s.drill.frames[s.drillFrameIdx];
    let boxes = [
      React.createElement("div", {}, s.drill.name),
      React.createElement("div", {}, s.drill.description)
    ];
    if (s.drillFrameStage == "main") {
      boxes.push(
	React.createElement("div", {}, f.question_comment)
      );
    } else if (s.drillFrameStage == "post") {
      boxes.push(
	React.createElement("div", {}, f.answer_comment)
      );
    }
    return React.createElement(
      "div",
      { style: hflexStyle },
      React.createElement(
	Board,
	{
	  pieces: s.pieces,
	  flipBoard: s.flipBoard,
	  selectedPieceSquare: s.selectedPieceSquare,
	  clickPieceFn: clickPiece,
	  clickSquareFn: clickSquare
	}
      ),
      React.createElement(
	"div",
	{ style: vflexStyle },
	...boxes
      )
    );
  };
}

document.addEventListener("keydown", (e) => {
  switch (e.code) {
  case "ArrowRight":
    advanceDrillFrame();
    break;
  }
});

window.addEventListener("DOMContentLoaded", () => {
  const div = document.getElementById("main");
  const page = React.createElement(Page, appState);
  ReactDOM.render(page, div);  
  beginDrill(exampleDrill);
});
