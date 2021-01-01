import React, { Component } from "react";

const filterStyle = {
  maxWidth: "150px",
  height: "30px",
  padding: "7px"
};

const andStyle = {
  display: "flex",
  flexDirection: "row"
};

const orStyle = {
  display: "flex",
  flexDirection: "column"
};

const Filter = (props) => {
  let { tag } = props;
  return React.createElement(
    "button",
    { style: filterStyle },
    tag
  );
};

const AndCondition = (props) => {
  let { filters,
        newFilter } = props;
  return React.createElement(
    "div",
    { style: andStyle },
    ...filters.map(
      tag => Filter({tag: tag}) 
    ),
    React.createElement(
      "button",
      { style: filterStyle,
	onClick: newFilter },
      "and"
    )
  );
};

export const FilterDrillsPanel = (props) => {
  let { andConditions,
        newAndCondition,
        newFilter } = props;
  return React.createElement(
    "div",
    { style: orStyle },
    ...andConditions.map(
      (filters, idx) => AndCondition({
	filters: filters,
	newFilter: () => { newFilter(idx); }
      }) 
    ),
    React.createElement(
      "button",
      { style: filterStyle,
	onClick: newAndCondition },
      "or"
    )
  );
};
