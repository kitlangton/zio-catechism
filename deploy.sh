sbt frontendJS/fullOptJS
npm run build:prod
cp dist/index.html dist/200.html
npx surge ./dist 'zio.surge.sh'
