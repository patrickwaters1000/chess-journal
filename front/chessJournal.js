import React, { Component } from "react";
import ReactDOM from "react-dom";
import Board from "./Board.js";
import Line from "./Line.js";

const hflexStyle = {display: "flex", flexDirection: "row"}
const vflexStyle = {display: "flex", flexDirection: "column"}

var handle = null;

const deepCopy = o => JSON.parse(JSON.stringify(o));

const syncAppState = () => {
  handle.setState(deepCopy(appState));
};

// NOTE the variation stack is a list, of maps;
// each map has keys `ply` and `line`.
var appState = {
  pieces: [],
  gameIdx: null,
  gamesMetadata: [],
  variationStack: [],
  editingVariation: false,
  selectedPieceSquare: null,
  flipBoard: false
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


const GameMetadata1 = (props) => React.createElement(
  "div",
  {
    style: {
      ...hflexStyle,
      margin: "20px",
    },
    onClick: props.onClick
  },
  React.createElement(
    "div",
    {style: vflexStyle },
    `white: ${props.white}`,
    `black: ${props.black}`
  ),
  React.createElement(
    "div",
    {style: vflexStyle },
    `result: ${props.result}`,
    `date: ${props.date}`
  )
);

const getResultStr = (result) => {
  if (result == 0.0) {
    return "0 - 1";
  } else if (result == 1.0) {
    return "1 - 0";
  } else if (result == 0.5) {
    return "1/2 - 1/2";
  } else {
    return "???";
  }
};

const GameMetadata2 = (props) => React.createElement(
  "div",
  {
    style: {
      ...hflexStyle,
      margin: "5px",
      padding: "10px",
      borderStyle: "solid"
    },
    onClick: props.onClick
  },
  `${props.white} vs ${props.black} `
    + `/ ${getResultStr(props.result)} `
    + `/ ${props.date.substring(0, 10)}`
);

const MetadataPanel = (props) => {
  return React.createElement(
    "div",
    {
      style: {
      ...vflexStyle
      }
    },
    props.gamesMetadata.map(row => GameMetadata2({
      ...row,
      onClick: () => {
	if (!props.gameIdx) {
	  loadGame(row.id);
	}
      }
    }))
  );
};

const Button = (props) => {
  return React.createElement(
    "button",
    { onClick: props.onClick },
    props.text
  );
};

const EditStuffButtons = (props) => {
  let buttons = [];
  if (props.editingVariation) {
    buttons.push(Button({ onClick: finalizeVariation, text: "Submit" }));
    buttons.push(Button({ onClick: abandonVariation, text: "Cancel" }));
  } else {
    buttons.push(
      Button({ onClick: initializeVariation, text: "New variation"})
    );
  }
  buttons.push(Button({ onClick: flipBoard, text: "Flip board" }));
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

const GamePanel = (props) => {
  let gameMetadata = props.gamesMetadata[props.gameIdx];
  let variations = props.variationStack.map(p => {
    let p2 = deepCopy(p);
    p2.stepIntoVariationFn = loadVariation;
    return React.createElement(Line, p2);
  });
  return React.createElement(
    "div",
    {
      style: vflexStyle
    },
    React.createElement(GameMetadata2, gameMetadata),
    EditStuffButtons({editingVariation: props.editingVariation}),
    ...variations
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
    let panels;
    if (s.gameIdx) {
      panels = [GamePanel(s)];
    } else {
      panels = [MetadataPanel(s)];
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
      ...panels
    );
  };
}

const getCurrentPosition = () => {
  let vs = appState.variationStack;
  if (vs.length > 0) {
    let v = vs[vs.length - 1];
    let p = v.moves[v.ply];
    return {
      fen: p.fen,
      full_move_counter: p.full_move_counter,
      active_color: p.active_color
    };
  }
};

const flipBoard = () => {
  appState.flipBoard = !appState.flipBoard;
  syncAppState();
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
      loadGame(appState.gamesMetadata[appState.gameIdx].id);
    });
  }
};

const setMetadata = (data) => {
  appState.gamesMetadata = data;
  syncAppState();
};

const getGameIdx = (gameId) => {
  let v;
  appState.gamesMetadata.forEach((row, idx) => {
    if (row.id == gameId) {
      v = idx;
    }
  });
  // DOES NOT WORK IF RETURN INSIDE `forEach`
  return v;
};

const loadGame = (gameId) => {
  console.log("Fetching game ", gameId);
  fetch(`game?id=${gameId}`)
    .then(response => response.json())
    .then(data => {
      appState.gameIdx = getGameIdx(gameId);
      let { line } = data;
      appState.variationStack = [{
	moves: line.moves,
	line_id: line.id,
	ply: 0
      }];
      setBoard();
      syncAppState();
      console.log("After loading game, gameIdx = ", appState.gameIdx);
    });
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
  console.log("stepping into line with comment ${line.comment}");
  vs.push({
    ply: 0,
    moves: line.moves,
    line_id: line.id,
    comment: line.comment
  });
  setBoard();
  syncAppState();
};

const loadVariation = (lineId) => {
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
  fetch('games-metadata')
    .then(response => response.json())
    .then(setMetadata);
});
