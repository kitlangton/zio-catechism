import { resolve } from 'path'

const scalaVersion = '2.13'
// const scalaVersion = '3.0.0-RC1'

// https://vitejs.dev/config/
export default ({ mode }) => {
  const mainJS = `modules/frontend/target/scala-${scalaVersion}/formula-${mode === 'production' ? 'opt' : 'fastopt'}/main.js`
  console.log('mainJS', mainJS)
  return {
    publicDir: './modules/frontend/src/main/static/public',
    resolve: {
      alias: {
        'stylesheets': resolve(__dirname, './modules/frontend/src/static/stylesheets'),
      }
    }
  }
}
