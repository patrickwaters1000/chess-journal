import React, { Component } from "react";

const deepCopy = o => JSON.parse(JSON.stringify(o));

const lineStyle = {
  margin: "20px",
  display: "flex",
  flexWrap: "wrap",
  maxWidth: "300px"
};

const moveStyle = {
  marginLeft: "5px",
  display: "flex"
  //paddingLeft: "5px"
}

// INSANE currently full_move_counter and active_color refer to the
// position after the move, and therefore are not correct for printing
// the move san.
const formatMoveText = (props, idx) => {
  let { san,
	full_move_counter,
	active_color,
      } = props;
  let adjusted_active_color = (active_color == "w" ? "b" : "w");
  let adjusted_full_move_counter = full_move_counter;
  if (adjusted_active_color == "b") {
    adjusted_full_move_counter -= 1;
  }
  let text;
  if (adjusted_active_color == "w") {
    text = `${adjusted_full_move_counter}. ${san}`;
  } else if (idx == 1) {
    text = `${adjusted_full_move_counter}. .. ${san}`;
  } else {
    text = san;
  }
  return text;
};

const Move = (props, idx, isCurrentMove) => {
  let { san,
	full_move_counter,
	active_color,
	stepIntoVariationFn,
	line_id
      } = props;
  let text = formatMoveText(props, idx);
  let style = deepCopy(moveStyle);
  if (isCurrentMove) {
    style.backgroundColor = "#cc0000";
  }
  return React.createElement(
    "div",
    {
      style: style,
      onClick: () => { stepIntoVariationFn(line_id); }
    },
    text,
  );
};

const MoveGroup = (p, idx, isCurrentMove) => {
  let move = Move(p, idx, isCurrentMove);
  let variations = p.variations.map(v => {
    return Move(
      {
	san: v.san,
	line_id: v.line_id,
	full_move_counter: p.full_move_counter,
	active_color: p.active_color,
	stepIntoVariationFn: p.stepIntoVariationFn,
      },
      // Somewhat awkwardly, the first move has idx 1 not 0.
      1,
      false
    );
  });
  if (variations.length == 0) {
    return [move];
  } else {
    return [
      move,
      "(",
      ...variations,
      ")"
    ];  
  }
};
  
export default class Line extends React.Component {
  render () {
    let {
      moves, ply, stepIntoVariationFn
    } = this.props;
    let children = [];
    // First element of line is initial pos w/ no move
    moves.slice(1).forEach((data, idx) => {
      let p = deepCopy(data);
      p.stepIntoVariationFn = stepIntoVariationFn;
      children = [
	...children,
	...MoveGroup(
	  p,
	  idx + 1,
	  idx + 1 == ply,
	)
      ];
    });
    return React.createElement(
      "div",
      {
	height: 600,
	width: 300,
	style: lineStyle
      },
      ...children
    );
  }
}
