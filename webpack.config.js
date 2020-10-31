const path = require('path');
const HtmlWebPackPlugin = require("html-webpack-plugin");

const page = 'chessJournal';

module.exports = {
  entry: `./front/${page}.js`,
  output: {
    filename: `${page}.js`,
    path: path.resolve(__dirname, 'dist'),
  },
  module: {
    rules: [
      /*{
        test: /\.(js|jsx)$/,
        exclude: /node_modules/,
        use: {
          loader: "babel-loader"
        }
      },*/
      {
        test: /\.html$/,
        use: [
          {
            loader: "html-loader"
          }
        ]
      }
    ]
  },
  plugins: [
    new HtmlWebPackPlugin({
      template: `./front/${page}.html`,
      filename: `./${page}.html`, // automatically prefixed with output path
      //hash: true
      inject: false
    })
  ],
  devtool: 'eval-source-map'
};
