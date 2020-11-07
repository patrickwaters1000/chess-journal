import React, { Component } from "react";

const deepCopy = o => JSON.parse(JSON.stringify(o));

const lineStyle = {
  display: "flex",
  flexWrap: "wrap",
  maxWidth: "300px"
};

const moveStyle = {
  marginLeft: "5px",
  display: "flex"
  //paddingLeft: "5px"
}

const formatMoveText = (props, idx) => {
  let { san,
	full_move_counter,
	active_color,
      } = props;
  let text;
  if (active_color == "w") {
    text = `${full_move_counter}. ${san}`;
  } else if (idx == 0) {
    text = `${full_move_counter}. .. ${san}`;
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
      0,
      false
    );
  });
  if (variations.length == 0) {
    return move;
  } else {
    return React.createElement(
      "div",
      { style: moveStyle },
      move,
      "(",
      ...variations,
      ")"
    );
  }
};
  
export default class Line extends React.Component {
  render () {
    let {
      line, ply, stepIntoVariationFn
    } = this.props;
    return React.createElement(
      "div",
      {
	height: 600,
	width: 300,
	style: lineStyle
      },
      ...line.map((data, idx) => {
	let p = deepCopy(data);
	p.stepIntoVariationFn = stepIntoVariationFn;
	return MoveGroup(
	  p,
	  idx,
	  idx == ply - 1,
	);
      })
    );
  }
}
