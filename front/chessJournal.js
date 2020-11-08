import React, { Component } from "react";
import ReactDOM from "react-dom";
import Board from "./Board.js";
import Line from "./Line.js";
//import './chessJournal.css';

const hflexStyle = {display: "flex", flexDirection: "row"}
const vflexStyle = {display: "flex", flexDirection: "column"}

var handle = null;

const deepCopy = o => JSON.parse(JSON.stringify(o));

const syncAppState = () => {
  //console.log("Syncing state to ", JSON.stringify(appState));
  handle.setState(deepCopy(appState));
};

// NOTE the variation stack is a list, of maps;
// each map has keys `ply` and `line`.
var appState = {
  pieces: [],
  game: {white: "", black: "", date: "", result: ""},
  variationStack: [],
  editingVariation: false,
  selectedPieceSquare: null
};

const logState = (msg) => {
  console.log(msg, JSON.stringify(appState));
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
  {
    style: {
      ...vflexStyle,
      margin: "20px"
    }
  },
  React.createElement("div", null, `white: ${props.white}`),
  React.createElement("div", null, `black: ${props.black}`),
  React.createElement("div", null, `result: ${props.result}`),
  React.createElement("div", null, `date: ${props.date}`)
);


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

const Button = (props) => {
  return React.createElement(
    "button",
    { onClick: props.onClick },
    props.text
  );
};

const EditStuffButtons = (props) => {
  let buttons;
  if (props.editingVariation) {
    buttons = [
      Button({ onClick: finalizeVariation, text: "Submit" }),
      Button({ onClick: abandonVariation, text: "Cancel" })
    ];
  } else {
    buttons = [
      Button({ onClick: initializeVariation, text: "New variation"})
    ];
  }
  return React.createElement(
    "div",
    {
      style: {
	...hflexStyle,
	margin: "20px"
      }
    },
    ...buttons
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
    let variations = s.variationStack.map(p => {
      let p2 = deepCopy(p);
      p2.stepIntoVariationFn = loadVariation;
      return React.createElement(Line, p2);
    });
    return React.createElement(
      "div",
      //{className: "hflex"},
      {style: hflexStyle},
      React.createElement(
	Board,
	{
	  pieces: s.pieces,
	  selectedPieceSquare: s.selectedPieceSquare,
	  clickPieceFn: clickPiece,
	  clickSquareFn: clickSquare
	}
      ),
      React.createElement(
	"div",
	//{className: "vflex"},
	{style: vflexStyle},
	React.createElement(Metadata, s.game),
	EditStuffButtons({editingVariation: s.editingVariation}),
	...variations
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

const getCurrentPosition = () => {
  let vs = appState.variationStack;
  if (vs.length > 0) {
    let v = vs[vs.length - 1];
    /*console.log(
      "About to get current position. App state = ",
      JSON.stringify(appState)
    );*/
    let p = v.moves[v.ply];
    return {
      fen: p.fen,
      full_move_counter: p.full_move_counter,
      active_color: p.active_color
    };
  }
};

const pushMove = (move) => {
  let vs = appState.variationStack;
  if (vs.length > 0) {
    let v = vs[vs.length - 1];
    v.moves.push(move);
    v.ply = v.moves.length - 1;
  }
};

const clickPiece = (color, square) => {
  let active_color = getCurrentPosition().active_color;
  if (color == active_color) {
    if (appState.selectedPieceSquare != square) {
      appState.selectedPieceSquare = square;
    } else {
      appState.selectedPieceSquare = null;
    }
    syncAppState();
  }
};

const tryMove = (fromSquare, toSquare) => {
  fetch('move', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json;charset=utf-8' },
    body: JSON.stringify({
      from: fromSquare,
      to: toSquare,
      fen: getCurrentPosition().fen
    })
  }).then(response => response.json())
    .then(move => {
      if (move) {
	pushMove(move);
	setBoard();
	appState.selectedPieceSquare = null;
	syncAppState();
	/*console.log(
	  `Adding move ${JSON.stringify(move)} to variation.`
	  + `App state = ${JSON.stringify(appState)}.`
	);*/
      }
    });
};

const clickSquare = (toSquare) => {
  let fromSquare = appState.selectedPieceSquare;
  if (appState.selectedPieceSquare != null) {
    tryMove(fromSquare, toSquare);
  }
}

const initializeVariation = () => {
  //logState("Before initializing variation: "); 
  let vs = appState.variationStack;
  let depth = vs.length;
  if (depth > 0) {
    let v = vs[depth - 1]
    if (v.ply > 0) {
      v.ply -= 1;
      let m = deepCopy(v.moves[v.ply]);
      m.san = null;
      appState.variationStack.push({ply: 0, moves: [m], line_id: null});
      appState.editingVariation = true;
      setBoard();
      syncAppState();
      //logState("After initializing variation: "); 
    }
  }
};

const abandonVariation = () => {
  let vs = appState.variationStack;
  let depth = vs.length;
  if (depth > 0) {
    appState.variationStack = vs.slice(0, depth - 1);
    appState.editingVariation = false;
    setBoard();
    syncAppState();
  }
};

const finalizeVariation = () => {
  logState("About to finalize variation with state: ");
  let vs = appState.variationStack;
  if (vs.length > 0) {
    let l = vs[vs.length - 1].moves;
    let data = {
      fen: l[0].fen,
      san_seq: l.slice(1).map(d => d.san),
      comment_text: window.prompt("Variation comment:")
    };
    console.log(
      "Submitting new variation: ",
      JSON.stringify(data)
    );
    fetch('new-annotation', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json;charset=utf-8' },
      body: JSON.stringify(data)
    }).then(resp => {
      console.log("Response to new-variation req: ", resp.text());
      appState.editingVariation = false;
      fetch('game')
	.then(response => response.json())
	.then(setGame)
    });
  }
};

const setGame = (data) => {
  //console.log("Received game: ", JSON.stringify(data));
  let { white, black, date, result, line } = data;
  appState.game = {
    white: white,
    black: black,
    date: date,
    result: result
  };
  appState.variationStack = [{
    moves: line.moves,
    line_id: line.id,
    ply: 0
  }];
  //appState.comments = data.comments;
  setBoard();
  syncAppState();
};

const setBoard = () => {
  let vs = appState.variationStack;
  let depth = vs.length;
  if (depth > 0) {
    let v = vs[depth - 1];
    let data = v.moves[v.ply];
    let board = parseFEN(data.fen);
    appState.pieces = getPieces(board);
  }
};

const nextMove = () => {
  let vs = appState.variationStack;
  let depth = vs.length;
  if (depth > 0) {
    let v = vs[depth - 1];
    if (v.ply + 1 < v.moves.length) {
      v.ply += 1;
      setBoard();
      syncAppState();
    }
  }
};

const prevMove = () => {
  let vs = appState.variationStack;
  let depth = vs.length;
  if (depth > 0) {
    let v = vs[depth - 1];
    if (v.ply > 0) {
      v.ply -= 1;
    } else {
      appState.variationStack = vs.slice(0, depth - 1);
    }
    setBoard();
    syncAppState();
  }
};

/*const gotoMove = (idx) => {
  let n = appState.line.length;
  if (0 <= idx < n) {
    appState.ply = idx;
    setBoard();
    syncAppState();
  }
}*/

const stepIntoVariation = (line) => {
  let vs = appState.variationStack;
  vs.push({ply: 0, moves: line.moves, line_id: line.id});
  setBoard();
  syncAppState();
};

const loadVariation = (lineId) => {
  //console.log("Fetching variation ", JSON.stringify(lineId));
  fetch(`line?id=${lineId}`)
    .then(response => response.json())
    .then(stepIntoVariation);
};

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
