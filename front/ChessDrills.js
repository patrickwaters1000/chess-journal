import React, { Component } from "react";
import ReactDOM from "react-dom";
import Board from "./Board.js";
import { getPiecesFromFen } from "./Fen.js";
import { drills } from "./load-drills.js";

const hflexStyle = {display: "flex", flexDirection: "row"}
const vflexStyle = {display: "flex", flexDirection: "column"}

var handle = null;

const deepCopy = o => JSON.parse(JSON.stringify(o));

const syncAppState = () => {
  handle.setState(deepCopy(appState));
};

//const randomPermutation = (n) => {
// use array splice
// the point is to shuffle the drills.
//}  

const nullDrill = {
  name: "",
  description: "",
  tags: [],
  frames: [{
    fen0: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    active_color: "w",
    comment: ""
  }]
};

const appState = {
  pieces: [],
  drillIdx: 0,
  drill: nullDrill,
  drillFrameIdx: 0,
  drillFrameStage: null,
  selectedPieceSquare: null,
  active_color: null,
  flipBoard: null,
};

const logDrill = (msg) => {
  console.log(msg, JSON.stringify(appState.drill));
};

const clickPiece = (color, square) => {
  console.log("Clicked a piece!");
  let s = appState;
  if (s.drillFrameStage == "main"
      && color == s.active_color) {
    if (s.selectedPieceSquare != square) {
      s.selectedPieceSquare = square;
    } else {
      s.selectedPieceSquare = null;
    }
    syncAppState();
  } 
};

const clickSquare = (toSquare) => {
  let s = appState;
  let fromSquare = s.selectedPieceSquare;
  if (s.drillFrameStage == "main"
      && s.selectedPieceSquare != null) {
    tryMove(fromSquare, toSquare);
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
    s.selectedPieceSquare = null;
    syncAppState();
  }
};

const beginDrill = () => {
  let n = drills.length;
  let idx = appState.drillIdx % n;
  //let idx = Math.floor(Math.random() * n);
  appState.drill = drills[idx];
  logDrill("Beginning this drill:\n");
  appState.drillFrameIdx = 0;
  drillFramePreStage(
    1000 // Wait 1s
  );
};

const advanceDrillFrame = () => {
  let s = appState;
  let maxFrameIdx = s.drill.frames.length - 1;
  if (s.drillFrameStage == "post") {
    if (s.drillFrameIdx < maxFrameIdx) {
      s.drillFrameIdx += 1;
      drillFramePreStage(
	0 // No delay
      );
    } else {
      s.drillIdx += 1;
      beginDrill();
    }
  }
};

const drillFramePreStage = (delayMillis) => {
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
    delayMillis
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


const boxStyle = {
  margin: "5px",
  padding: "10px",
  borderStyle: "solid",
  width: "300px"
};

const makeBox = (msg, height) => {
  return React.createElement(
    "div",
    {
      style: {
	...boxStyle,
	height: `${height}px`
      }
    },
    msg
  );
};

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
      makeBox(s.drill.name, 100),
      makeBox(s.drill.description, 100),
      makeBox(f.comment, 100)
    ];
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
  beginDrill();
});
