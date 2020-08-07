const path = require('path');
const _ = require('lodash');

const ExtractCssChunks = require('extract-css-chunks-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const PurgeCSSLaminarPlugin = require('purgecss-laminar-webpack-plugin').default;

const {
  CleanWebpackPlugin
} = require('clean-webpack-plugin');

const devBackend = 'http://127.0.0.1:30099';
const devServerPort = 30090;

const devServer = {
  hot: true,
  disableHostCheck: true,
  clientLogLevel: 'none',
  public: 'http://localhost',
  port: devServerPort,
  proxy: {
    '/api': {
      target: devBackend,
      changeOrigin: true,
      ws: true
    }
  },
  historyApiFallback: {
    index: ''
  }
};

function common(variables, mode) {
  const postcssOptions = {
    config: {
      path: path.resolve(
        __dirname,
        (mode === 'production') ?
        './postcss.prod.config.js' :
        './postcss.config.js'
      )
    }
  };

  return {
    mode: mode,
    resolve: {
      modules: [
        "node_modules",
        path.resolve(__dirname, "node_modules"),
        path.resolve(__dirname, "modules/frontend/vendor/"),
      ],
    },
    output: {
      publicPath: '/',
      filename: '[name].[hash].js',
      library: 'app',
      libraryTarget: 'var'
    },
    entry: [
      path.resolve(__dirname, './modules/frontend/src/static/stylesheets/main.scss'),
      path.resolve(__dirname, './modules/frontend/src/static/stylesheets/main.css')
    ],
    module: {
      rules: [{
          test: /\.js$/,
          enforce: 'pre',
          use: [{
            loader: 'scalajs-friendly-source-map-loader',
            options: {
              name: '[name].[contenthash:8].[ext]',
              bundleHttp: false
            }
          }]
        },
        {
          test: /\.css$/,
          use: [{
              loader: ExtractCssChunks.loader,
              options: {
                filename: '[name].[contenthash:8].[ext]'
              }
            },
            {
              loader: 'css-loader'
            },
            {
              loader: "postcss-loader",
              options: postcssOptions
            }
          ]
        },
        {
          test: /\.scss$/,
          use: [{
              loader: ExtractCssChunks.loader,
              options: {
                filename: '[name].[contenthash:8].[ext]'
              }
            },
            {
              loader: 'css-loader'
            },
            {
              loader: "postcss-loader",
              options: postcssOptions
            },
            {
              loader: 'sass-loader'
            }
          ]
        },
        {
          test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
          use: [{
            loader: 'file-loader',
            options: {
              name: '[name].[ext]',
              outputPath: 'fonts/'
            }
          }]
        },
        {
          test: /\.(png|jpg)(\?v=\d+\.\d+\.\d+)?$/,
          use: [{
            loader: 'file-loader',
            options: {
              name: '[name].[ext]',
              outputPath: 'images/'
            }
          }]
        },
        {
          test: /\.(txt)(\?v=\d+\.\d+\.\d+)?$/,
          use: [{
            loader: 'file-loader'
          }]
        }
      ]
    },
    plugins: [
      new HtmlWebpackPlugin({
        filename: 'index.html',
        template: './modules/frontend/src/static/html/index.html',
        minify: false,
        inject: 'head',
        config: variables
      }),

      new ExtractCssChunks({
        filename: '[name].[hash].css',
        chunkFilename: '[id].css'
      }),
      new CopyWebpackPlugin([{
          from: './modules/frontend/src/static/images',
          to: 'images'
        },
        {
          from: './modules/frontend/src/static/robots.txt',
          to: '[name].[ext]'
        },
        {
          from: './modules/frontend/src/static/data/*.json',
          to: '[name].[ext]'
        },
        {
          from: './modules/frontend/src/static/javascripts/*.min.js.map',
          to: '[name].[ext]'
        },
        {
          from: './modules/frontend/src/static/javascripts/*.min.js',
          to: '[name].[ext]'
        },
      ])
    ],
    devServer
  }
}

const dev = {
  entry: [
    path.resolve(
      __dirname,
      "./modules/frontend/.js/target/scala-2.13/frontend-fastopt.js"
    )
  ]
};

const prod = {
  entry: [
    path.resolve(__dirname, './modules/frontend/.js/target/scala-2.13/frontend-opt.js'),
  ],
  plugins: [
    new CleanWebpackPlugin(),
    // new PurgeCSSLaminarPlugin(
      // {
      //   stringFilters: {
      //     minLength: 2,
      //     maxLength: 30,
      //     skipAllUpperCase: true,
      //     onlyAllLowerCase: true,
      //   },
      //   debug: false
      // }
    // )
  ]
};


function customizer(objValue, srcValue) {
  if (_.isArray(objValue)) {
    return objValue.concat(srcValue);
  }
}

module.exports = function (env) {
  switch (process.env.npm_lifecycle_event) {
    case 'build:prod':
      return _.mergeWith({}, common(require('./variables.prod.js'), 'production'), prod, customizer);

    default:
      console.log('using dev config');
      return _.mergeWith({}, common(require('./variables.dev.js'), 'development'), dev, customizer);
  }
};
