import React, { Component } from "react";
import ReactDOM from "react-dom";
import Board from "./Board.js";
//import './chessJournal.css';

const hflexStyle = {display: "flex", flexDirection: "row"}
const vflexStyle = {display: "flex", flexDirection: "column"}

var handle = null;

const deepCopy = o => JSON.parse(JSON.stringify(o));

const syncAppState = () => {
  //console.log("Syncing state to ", JSON.stringify(appState));
  handle.setState(deepCopy(appState));
};

var appState = {
  pieces: [],
  game: {white: "", black: "", date: "", result: ""},
  line: [],
  ply: 0
  //comments: [],
  //selectedCommentIdx: -1,
  //currentlyEditingComment: false
};

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


const Metadata = props => React.createElement(
  "div",
  //{className: "vflex"},
  {style: vflexStyle},
  React.createElement("p", null, `white: ${props.white}`),
  React.createElement("p", null, `black: ${props.black}`),
  React.createElement("p", null, `result: ${props.result}`),
  React.createElement("p", null, `date: ${props.date}`)
);

/*const strfSingleMove = (fullMoveCounter, activeColor, san) => {
  return (activeColor == "w"
	  ? `${fullMoveCounter}. ${san}`
	  : `${fullMoveCounter}. .. ${san}`);
};*/

/*const getCommentSummary = (comment) => {
  console.log(`Comment: ${JSON.stringify(comment)}`);
  let { text, full_move_counter, active_color, san } = comment;
  let s1 = strfSingleMove(
    full_move_counter, active_color, san[0]);
  let s2 = (text.length <= 13 ? text : `${text.substring(0,10)}...`);
  return `${s1} ${s2}`;
};*/

/*
const CommentButton = props => React.createElement(
  "button",
  {
    onClick: () => {
      appState.selectedCommentIdx = props.idx;
      gotoMove(props.full_move_counter, props.active_color);
      syncAppState();
    }
  },
  getCommentSummary(props)
);
*/

/*
const CommentButtons = props => {
  let { comments } = props;
  return React.createElement(
    "div",
    //{className: "vflex"},
    {style: vflexStyle},
    ...props.comments.map(
      (comment, idx) => React.createElement(
	CommentButton,
	{ idx: idx,
	  text: comment.text,
	  san: comment.san,
	  active_color: comment.active_color,
	  full_move_counter: comment.full_move_counter })
    )
  );
};
*/

/*
const getVariationString = (fullMoveCounter, activeColor, san) => {
  var i0, acc;
  if (activeColor == "w") {
    i0 = 2;
    acc = `${fullMoveCounter}. ${san[0]} ${san[1]}`;
  } else {
    i0 = 1;
    acc = `${fullMoveCounter}. .. ${san[1]}`;
  }
    let m = fullMoveCounter + 1;
  for (let i = i0; i < san.length; i += 2) {
    acc += ` ${m}. ${san[i]} ${san[i+1]}`
    m += 1;
  }
  return acc;
};
*/

/*
const CommentText = props => {
  return React.createElement(
    "div",
    {style: vflexStyle},
    React.createElement(
      "label", {for: "annotation-box"}, "annotation:"),
    React.createElement(
      "textarea",
      {id: "annotation-box", rows: 4, cols: 4, value: props.text}
    ),
    React.createElement(
      "label", {for: "variation-box"}, "variation:"),
    React.createElement(
      "textarea",
      {
	id: "variation-box",
	rows: 4,
	cols: 4,
	value: getVariationString(
	  props.fullMoveCounter,
	  props.activeColor,
	  props.san
	)
      })
  );
};
*/

class Page extends React.Component {
  constructor(props) {
    super(props);
    this.state = props;
    handle = this;
  };
  
  render() {
    let s = this.state;
    //let i = s.selectedCommentIdx;
    /*let san, text, active_color, full_move_counter;
    if (i == -1) {
      san = s.san;
      text = "";
      active_color = "w";
      full_move_counter = 1;
    } else {
      san = s.comments[i].san;
      text = s.comments[i].text;
      active_color = s.comments[i].active_color;
      full_move_counter = s.comments[i].full_move_counter;
    }*/
    return React.createElement(
      "div",
      //{className: "hflex"},
      {style: hflexStyle},
      React.createElement(Board, {pieces: s.pieces}),
      React.createElement(
	"div",
	//{className: "vflex"},
	{style: vflexStyle},
	React.createElement(Metadata, s.game),
	//React.createElement(CommentButtons, {comments: s.comments}),
	/*React.createElement(
	  CommentText,
	  { san: san,
	    text: text,
	    activeColor: active_color,
	    fullMoveCounter: full_move_counter })*/
      )
    );
  };
}

const setGame = (data) => {
  //console.log("Received game: ", JSON.stringify(data));
  let { white, black, date, result, line } = data;
  appState.game = {
    white: white,
    black: black,
    date: date,
    result: result
  };
  appState.line = line;
  appState.ply = 0;
  //appState.comments = data.comments;
  setBoard();
  syncAppState();
};

// INCOMPLETE
// not used when loading a game
// finish when needed
const setLine = (data) => {
  null;
};

const setBoard = () => {
  /*console.log(
    "Trying to set board. App state = ",
    JSON.stringify(appState));*/
  let data = appState.line[appState.ply];
  let board = parseFEN(data.fen);
  appState.pieces = getPieces(board);
};

const nextMove = () => {
  let n = appState.line.length;
  if (appState.ply < n - 1) {
    appState.ply += 1;
    setBoard();
    syncAppState();
  }
};

const prevMove = () => {
  if (appState.ply > 0) {
    appState.ply -= 1;
    setBoard();
    syncAppState();
  }
};

const gotoMove = (idx) => {
  let n = appState.line.length;
  if (0 <= idx < n) {
    appState.ply = idx;
    setBoard();
    syncAppState();
  }
}

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

window.addEventListener("DOMContentLoaded", () => {
  const div = document.getElementById("main");
  const page = React.createElement(Page, appState);
  ReactDOM.render(page, div);  
  fetch('game')
    .then(response => response.json())
    .then(setGame);
});
