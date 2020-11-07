import React, { Component } from "react";
import ReactDOM from "react-dom";
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
  metadata: {white: "", black: "", date: "", result: ""},
  san: [],
  comments: [],
  selectedCommentIdx: -1,
  currentlyEditingComment: false
};

const dx = window.innerHeight / 8;
const dy = window.innerHeight / 8;

const getPieceFn = points => {
  return props => {
    const pointsStr = points.map(p => {
      let [x, y] = p;
      let xScaled = (props.file + x ) * dx;
      let yScaled = (7 - props.rank + 1 - y) * dy;
      return `${xScaled},${yScaled}`;
    }).join(" ");
    return React.createElement(
      "polygon",
      {
	points: pointsStr,
	fill: props.color,
	stroke: "#000000"
      }
    );
  };
};

const Pawn = getPieceFn([
  [0.3, 0.1],
  [0.45, 0.2],
  [0.45, 0.3],
  [0.45, 0.4],
  [0.37, 0.4],
  [0.45, 0.5],
  [0.5, 0.6],
  [0.55, 0.5],
  [0.63, 0.4],
  [0.55, 0.4],
  [0.55, 0.3],
  [0.55, 0.2],
  [0.7, 0.1]
]);

const Knight = getPieceFn([
  [0.23, 0.1],
  [0.3, 0.3],
  [0.4, 0.4],
  [0.45, 0.6],
  [0.25, 0.45],
  [0.15, 0.5],
  [0.2, 0.6],
  [0.3, 0.7],
  [0.33, 0.8],
  [0.4, 0.75],
  [0.6, 0.7],
  [0.7,0.55],
  [0.66, 0.3],
  [0.73, 0.1]
]);

const Bishop = getPieceFn([
  [0.2, 0.1],
  [0.23, 0.15],
  [0.35, 0.2],
  [0.2, 0.5],
  [0.23, 0.6],
  [0.47, 0.8],
  [0.43, 0.84],
  [0.5, 0.88],
  [0.57, 0.84],
  [0.53, 0.8 ],
  [0.77, 0.6],
  [0.8, 0.5],
  [0.65, 0.2],
  [0.77, 0.15],
  [0.8, 0.1]
]);

const Rook = getPieceFn([
  [0.13, 0.1],
  [0.13, 0.15],
  [0.25, 0.15],
  [0.25, 0.55],
  [0.17, 0.55],
  [0.17, 0.7],
  [0.29, 0.7],
  [0.29, 0.65],
  [0.35, 0.65],
  [0.35, 0.7],
  [0.47, 0.7],
  [0.47, 0.65],
  [0.53, 0.65],
  [0.53, 0.7],
  [0.65, 0.7],
  [0.65, 0.65],
  [0.71, 0.65],
  [0.71, 0.7],
  [0.83, 0.7],
  [0.83, 0.55],
  [0.75, 0.55],
  [0.75, 0.15],
  [0.87, 0.15],
  [0.87, 0.1]
]);

const Queen = getPieceFn([
  [0.15, 0.1],
  [0.2,0.25],
  [0.1, 0.55],
  [0.25, 0.45],
  [0.2, 0.65],
  [0.4, 0.5],
  [0.5, 0.7],
  [0.6, 0.5],
  [0.8, 0.65],
  [0.75, 0.45],
  [0.9, 0.55],
  [0.8, 0.25],
  [0.85, 0.1]
]);

const King = getPieceFn([
  [0.15, 0.1],
  [0.15, 0.15],
  [0.25, 0.2],
  [0.15, 0.5],
  [0.25, 0.6],
  [0.475, 0.6],
  [0.475, 0.65],
  [0.425, 0.65],
  [0.425, 0.7],
  [0.475, 0.7],
  [0.475, 0.75],
  [0.525, 0.75],
  [0.525, 0.70],
  [0.575, 0.70],
  [0.575, 0.65],
  [0.525, 0.65],
  [0.525, 0.60],
  [0.75, 0.60],
  [0.85, 0.5],
  [0.75, 0.2],
  [0.85, 0.15],    
  [0.85, 0.1]
]);

//const getSquare = (i, j) => { }

const Board = props => {
  const squares = [];
  for (let i = 0; i < 8; i++) {
    for (let j = 0; j < 8; j++) {
      let square = React.createElement(
	"rect",
	{
	  fill: ((i + j) % 2 == 0 ? "#ffffb3" : "#00b33c"),
	  x: i * dx,
	  y: j * dy,
	  width: dx,
	  height: dy
	}
      );
      squares.push(square);
    }
  }
  return React.createElement(
    "svg",
    {
      width: 8 * dx,
      height: 8 * dy
    },
    ...squares,
    ...props.pieces.map(getPiece)
  );
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

const parseFEN = fen => {
  let board = fen.split(" ")[0];
  return board.split("/").map(parseRank);
};

const getPieces = board => {
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

const getPiece = piece => {
  let {pieceType, rank, file} = piece;
  return {
    'p': Pawn({rank: rank, file: file, color: "#000000"}),
    'P': Pawn({rank: rank, file: file, color: "#ffffff"}),
    'n': Knight({rank: rank, file: file, color: "#000000"}),
    'N': Knight({rank: rank, file: file, color: "#ffffff"}),
    'b': Bishop({rank: rank, file: file, color: "#000000"}),
    'B': Bishop({rank: rank, file: file, color: "#ffffff"}),
    'r': Rook({rank: rank, file: file, color: "#000000"}),
    'R': Rook({rank: rank, file: file, color: "#ffffff"}),
    'q': Queen({rank: rank, file: file, color: "#000000"}),
    'Q': Queen({rank: rank, file: file, color: "#ffffff"}),
    'k': King({rank: rank, file: file, color: "#000000"}),
    'K': King({rank: rank, file: file, color: "#ffffff"}),
  }[pieceType];
}

const Metadata = props => React.createElement(
  "div",
  //{className: "vflex"},
  {style: vflexStyle},
  React.createElement("p", null, `white: ${props.white}`),
  React.createElement("p", null, `black: ${props.black}`),
  React.createElement("p", null, `result: ${props.result}`),
  React.createElement("p", null, `date: ${props.date}`)
);

const strfSingleMove = (fullMoveCounter, activeColor, san) => {
  return (activeColor == "w"
	  ? `${fullMoveCounter}. ${san}`
	  : `${fullMoveCounter}. .. ${san}`);
};

const getCommentSummary = (comment) => {
  console.log(`Comment: ${JSON.stringify(comment)}`);
  let { text, full_move_counter, active_color, san } = comment;
  let s1 = strfSingleMove(
    full_move_counter, active_color, san[0]);
  let s2 = (text.length <= 13 ? text : `${text.substring(0,10)}...`);
  return `${s1} ${s2}`;
};

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

class Page extends React.Component {
  constructor(props) {
    super(props);
    this.state = props;
    handle = this;
  };
  
  render() {
    let s = this.state;
    let i = s.selectedCommentIdx;
    let san, text, active_color, full_move_counter;
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
    }
    return React.createElement(
      "div",
      //{className: "hflex"},
      {style: hflexStyle},
      React.createElement(Board, {pieces: s.pieces}),
      React.createElement(
	"div",
	//{className: "vflex"},
	{style: vflexStyle},
	React.createElement(Metadata, s.metadata),
	React.createElement(CommentButtons, {comments: s.comments}),
	React.createElement(
	  CommentText,
	  { san: san,
	    text: text,
	    activeColor: active_color,
	    fullMoveCounter: full_move_counter }),
      )
    );
  };
}

const setState = data => {
  console.log("Received: ", JSON.stringify(data));
  let board = parseFEN(data.fen);
  appState.pieces = getPieces(board);
  appState.metadata = {
    white: data.white,
    black: data.black,
    date: data.date,
    result: data.result
  };
  appState.san = data.san;
  appState.comments = data.comments;
  syncAppState();
};

const nextMove = () => {
  fetch('next-move', {method: 'POST'})
    .then(resp => resp.json())
    .then(setState);
};

const prevMove = () => {
  fetch('prev-move', {method: 'POST'})
    .then(resp => resp.json())
    .then(setState);
};

const gotoMove = (fmc, ac) => {
  fetch(
    `goto-move?fullMoveCounter=${fmc}&activeColor=${ac}`,
    {method: 'POST'}
  ).then(resp => resp.json()).then(setState);
}

//let keystrokes = 0;

document.addEventListener("keydown", (e) => {
  //keystrokes += 1;
  //if (keystrokes%2 == 0) {
  switch (e.code) {
  case "ArrowLeft":
    prevMove();
    break;
  case "ArrowRight":
    nextMove();
    break;
  }
  //}
});

window.addEventListener("DOMContentLoaded", () => {
  const div = document.getElementById("main");
  const page = React.createElement(Page, appState);
  ReactDOM.render(page, div);
  
  fetch('info')
    .then(response => response.json())
    .then(setState);
});
