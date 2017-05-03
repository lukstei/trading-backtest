orders <- read.csv('/var/folders/_5/jv4ptlps2ydb4_ptyj_l2y100000gn/T/out-7373128809679149089.csv') # insert output file here
statistics <- read.csv('/var/folders/_5/jv4ptlps2ydb4_ptyj_l2y100000gn/T/out-1984107031930922019.csv') # insert output file here
statistics$date = as.Date(statistics$date)

deposit = 15000
plot(statistics$date, (deposit+statistics$pl)/deposit-1, type='l', xlab = 'Date', ylab='Returns')

pctWin = sum(orders$pl > 0)/length(orders$pl)
print(paste('Win percentage: ', pctWin*100, '%'))

## specific to the cointegration strategy

kalmanStatistics = read.csv('/var/folders/_5/jv4ptlps2ydb4_ptyj_l2y100000gn/T/out-3935155810073028635.csv') # insert output file here
kalmanStatistics$date = as.Date(kalmanStatistics$date)

plot(kalmanStatistics$beta, type='l')

series = kalmanStatistics$y - kalmanStatistics$x * kalmanStatistics$beta - kalmanStatistics$alpha

plot(kalmanStatistics$date[50:length(series)], series[50:length(series)], type='l', xlab = 'Date', ylab='Residuals')
