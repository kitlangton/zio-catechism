const path = require('path');
const _ = require('lodash');

const ExtractCssChunks = require('extract-css-chunks-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin')
const CompressionPlugin = require('compression-webpack-plugin');

const scalaOutputPath = path.resolve(__dirname, './modules/frontend/.js/target/scala-2.13');

const devBackendPort = 30099;
const devServerPort = 30090;

const devBackend = `http://127.0.0.1:${devBackendPort}`;

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
  return {
    mode: mode,
    resolve: {
      modules: [
        "node_modules",
        path.resolve(__dirname, "node_modules")
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
      rules: [
        {
          test: /\.js$/,
          use: [
            {
              loader: "scalajs-friendly-source-map-loader",
              options: {
                name: '[name].[contenthash:8].[ext]',
                skipFileURLWarnings: true, // or false, default is true
                bundleHttp: true, // or false, default is true,
                cachePath: ".scala-js-sources", // cache dir name, exclude in .gitignore
                noisyCache: false, // whether http cache changes are output
                useCache: true, // false => remove any http cache processing
              }
            }],
          enforce: "pre",
          include: [scalaOutputPath],
        },
        {
          test: /\.js$/,
          use: ["source-map-loader"],
          enforce: "pre",
          // does not handle scala.js issued https: remote resources
          exclude: [/node_modules/, scalaOutputPath],
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
              options: {
                config: {
                  path: path.resolve(__dirname, './postcss.config.js')
                }
              }
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
              options: {
                config: {
                  path: path.resolve(__dirname, './postcss.config.js')
                }
              }
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
      new CopyWebpackPlugin(
        {
          patterns: [
            {
              from: './modules/frontend/src/static/images',
              to: 'images'
            },
            {
              from: './modules/frontend/src/static/robots.txt',
              to: '[name].[ext]'
            },
//            {
//              from: './modules/frontend/src/static/data/*.json',
//              to: '[name].[ext]'
//            }
          ]
        })
    ]
  }
}

const dev = {
  mode: 'development',
  entry: [
    path.resolve(__dirname, `${scalaOutputPath}/frontend-fastopt.js`)
  ],
  devtool: "cheap-module-eval-source-map"
};

const prod = {
  mode: 'production',
  entry: [
    path.resolve(__dirname, `${scalaOutputPath}/frontend-opt.js`),
  ],
  devtool: 'source-map',
  optimization: {
    minimize: true,
    minimizer: [new TerserPlugin()],
  },
  plugins: [
    new CleanWebpackPlugin(),
    new CompressionPlugin({
      test: /\.(js|css|html|svg|json|woff|woff2)$/,
      deleteOriginalAssets: false,
    }),
    new CompressionPlugin({
      filename: '[path].br[query]',
      algorithm: 'brotliCompress',
      test: /\.(js|css|html|svg|woff|woff2)$/,
      compressionOptions: {
        // zlib’s `level` option matches Brotli’s `BROTLI_PARAM_QUALITY` option.
        level: 11,
      },
      minRatio: 0.8,
      deleteOriginalAssets: false,
    }),
  ]
};


function customizer(objValue, srcValue) {
  if (_.isArray(objValue)) {
    return objValue.concat(srcValue);
  }
}

module.exports = function (env) {
  switch (process.env.npm_lifecycle_event) {
    case 'build':
    case 'build:prod':
      console.log('production build');
      return _.mergeWith({}, common(require('./variables.prod.js'), 'production'), prod, customizer);
    case 'build:dev':
      console.log('development build');
      return _.mergeWith({}, common(require('./variables.dev.js'), 'development'), dev, customizer);

    case 'start:prod':
      console.log('production dev server');
      return _.mergeWith({}, common(require('./variables.dev.js'), 'production'), prod, {devServer}, customizer);
    case 'start':
    case 'start:dev':
    default:
      console.log('development dev server');
      return _.mergeWith({}, common(require('./variables.dev.js'), 'development'), dev, {devServer}, customizer);
  }
}